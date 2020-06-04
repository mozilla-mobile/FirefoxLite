package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.first
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.domain.ReadContentHubItemUseCase
import org.mozilla.rocket.home.contenthub.domain.ShouldShowContentHubItemTextUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.domain.IsShoppingButtonEnabledUseCase
import org.mozilla.rocket.home.logoman.domain.DismissLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.GetLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.LastReadLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification
import org.mozilla.rocket.home.onboarding.CompleteHomeOnboardingUseCase
import org.mozilla.rocket.home.onboarding.IsNeedToShowHomeOnboardingUseCase
import org.mozilla.rocket.home.onboarding.domain.IsNewUserUseCase
import org.mozilla.rocket.home.onboarding.domain.SetShoppingSearchOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowShoppingSearchOnboardingUseCase
import org.mozilla.rocket.home.topsites.domain.GetTopSitesAbTestingUseCase
import org.mozilla.rocket.home.topsites.domain.GetTopSitesAbTestingUseCase.Companion.AB_TESTING_EXPERIMENT_NAME_TOP_SITES
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.TopSitesConfigsUseCase
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.domain.CheckInMissionUseCase
import org.mozilla.rocket.msrp.domain.CompleteJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.GetContentHubClickOnboardingEventUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.IsMsrpAvailableUseCase
import org.mozilla.rocket.msrp.domain.LastReadMissionIdUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.util.ToastMessage

class HomeViewModel(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase,
    private val getTopSitesAbTestingUseCase: GetTopSitesAbTestingUseCase,
    topSitesConfigsUseCase: TopSitesConfigsUseCase,
    private val pinTopSiteUseCase: PinTopSiteUseCase,
    private val removeTopSiteUseCase: RemoveTopSiteUseCase,
    getContentHubItemsUseCase: GetContentHubItemsUseCase,
    shouldShowContentHubItemTextUseCase: ShouldShowContentHubItemTextUseCase,
    private val readContentHubItemUseCase: ReadContentHubItemUseCase,
    private val getLogoManNotificationUseCase: GetLogoManNotificationUseCase,
    private val lastReadLogoManNotificationUseCase: LastReadLogoManNotificationUseCase,
    private val lastReadMissionIdUseCase: LastReadMissionIdUseCase,
    private val dismissLogoManNotificationUseCase: DismissLogoManNotificationUseCase,
    private val isMsrpAvailableUseCase: IsMsrpAvailableUseCase,
    private val isShoppingButtonEnabledUseCase: IsShoppingButtonEnabledUseCase,
    isNeedToShowHomeOnboardingUseCase: IsNeedToShowHomeOnboardingUseCase,
    completeHomeOnboardingUseCase: CompleteHomeOnboardingUseCase,
    private val checkInMissionUseCase: CheckInMissionUseCase,
    private val completeJoinMissionOnboardingUseCase: CompleteJoinMissionOnboardingUseCase,
    getContentHubClickOnboardingEventUseCase: GetContentHubClickOnboardingEventUseCase,
    refreshMissionsUseCase: RefreshMissionsUseCase,
    hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
    getIsFxAccountUseCase: GetIsFxAccountUseCase,
    shouldShowShoppingSearchOnboardingUseCase: ShouldShowShoppingSearchOnboardingUseCase,
    setShoppingSearchOnboardingIsShownUseCase: SetShoppingSearchOnboardingIsShownUseCase,
    isNewUserUseCase: IsNewUserUseCase
) : ViewModel() {

    val sitePages = MutableLiveData<List<SitePage>>()
    val topSitesPageIndex = MutableLiveData<Int>()
    val pinEnabled = MutableLiveData<Boolean>().apply { value = topSitesConfigsUseCase().isPinEnabled }
    val contentHubItems = getContentHubItemsUseCase()
    val shouldShowContentHubItemText = MutableLiveData<Boolean>().apply { value = shouldShowContentHubItemTextUseCase() }
    val logoManNotification = MediatorLiveData<StateNotification?>()
    val isAccountLayerVisible = MutableLiveData<Boolean>().apply { value = isMsrpAvailableUseCase() }
    val isShoppingSearchEnabled = MutableLiveData<Boolean>().apply { value = isShoppingButtonEnabledUseCase() }
    val hasUnreadMissions: LiveData<Boolean> = hasUnreadMissionsUseCase()
    val isFxAccount: LiveData<Boolean> = getIsFxAccountUseCase()

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val openShoppingSearch = SingleLiveEvent<Unit>()
    val openPrivateMode = SingleLiveEvent<Unit>()
    val openBrowser = SingleLiveEvent<String>()
    val showTopSiteMenu = SingleLiveEvent<ShowTopSiteMenuData>()
    val openContentPage = SingleLiveEvent<ContentHub.Item>()
    val showContentServicesOnboardingSpotlight = SingleLiveEvent<Unit>()
    val showToast = SingleLiveEvent<ToastMessage>()
    val openRewardPage = SingleLiveEvent<Unit>()
    val openProfilePage = SingleLiveEvent<Unit>()
    val showMissionCompleteDialog = SingleLiveEvent<Mission>()
    val openMissionDetailPage = SingleLiveEvent<Mission>()
    val showContentHubClickOnboarding = getContentHubClickOnboardingEventUseCase()
    val showShoppingSearchOnboardingSpotlight = SingleLiveEvent<Unit>()
    val dismissContentServiceOnboardingDialog = SingleLiveEvent<Unit>()
    val hideLogoManNotification = SingleLiveEvent<Unit>()
    val executeUriAction = SingleLiveEvent<String>()
    val showKeyboard = SingleLiveEvent<Unit>()

    private var logoManClickAction: GetLogoManNotificationUseCase.LogoManAction? = null
    private var logoManType: String? = null
    private var contentServicesOnboardingTimeSpent = 0L
    private var hasLoggedShowLogoman = false
    private var isFirstRun = isNewUserUseCase()

    init {
        initLogoManData()

        viewModelScope.launch {
            if (isMsrpAvailableUseCase()) {
                refreshMissionsUseCase()
            }
        }
        if (isNeedToShowHomeOnboardingUseCase()) {
            completeHomeOnboardingUseCase()
            contentServicesOnboardingTimeSpent = System.currentTimeMillis()
            if (isFirstRun) {
                TelemetryWrapper.showFirstRunContextualHint("onboarding_2_content_services_news_shopping_games")
            } else {
                TelemetryWrapper.showWhatsnewContextualHint("onboarding_2_content_services_news_shopping_games")
            }
            showContentServicesOnboardingSpotlight.call()
            // To prevent showing in app message when onboarding
            FirebaseHelper.getFirebase().setIamMessagesSuppressed(true)
        } else if (shouldShowShoppingSearchOnboardingUseCase()) {
            setShoppingSearchOnboardingIsShownUseCase()
            showShoppingSearchOnboardingSpotlight.call()
            // To prevent showing in app message when onboarding
            FirebaseHelper.getFirebase().setIamMessagesSuppressed(true)
        }
    }

    fun onContentServicesOnboardingSpotlightDismiss() {
        FirebaseHelper.getFirebase().setIamMessagesSuppressed(false)
    }

    fun onShoppingSearchOnboardingSpotlightDismiss() {
        FirebaseHelper.getFirebase().setIamMessagesSuppressed(false)
    }

    private fun initLogoManData() {
        logoManNotification.addSource(
            getLogoManNotificationUseCase().first()
                    .map {
                        logoManClickAction = it?.action
                        logoManType = it?.type
                        it?.run { StateNotification(it.toUiModel(), true) }
                    }
        ) {
            logoManNotification.value = it
        }
        val lastReadIdObserver = Observer<String> { lastReadId ->
            val showingNotification = logoManNotification.value
            if (showingNotification != null && showingNotification.notification.id == lastReadId) {
                hideLogoManNotification.call()
                logoManNotification.value = null
            }
        }
        logoManNotification.addSource(lastReadLogoManNotificationUseCase(), lastReadIdObserver)
        logoManNotification.addSource(lastReadMissionIdUseCase(), lastReadIdObserver)
    }

    private fun updateTopSitesData() = viewModelScope.launch {
        if (LocalAbTesting.isExperimentEnabled(AB_TESTING_EXPERIMENT_NAME_TOP_SITES) &&
                LocalAbTesting.checkAssignedBucket(AB_TESTING_EXPERIMENT_NAME_TOP_SITES) != null) {
            sitePages.value = getTopSitesAbTestingUseCase().toSitePages()
        } else {
            sitePages.value = getTopSitesUseCase().toSitePages()
        }
    }

    private fun List<Site>.toSitePages(): List<SitePage> = chunked(TOP_SITES_PER_PAGE)
            .take(TOP_SITES_MAX_PAGE_SIZE)
            .map { SitePage(it) }

    fun onPageForeground() {
        val logoManNotification = logoManNotification.value
        if (!hasLoggedShowLogoman && logoManNotification != null) {
            hasLoggedShowLogoman = true
            TelemetryWrapper.showLogoman(logoManType, logoManClickAction?.getLink(), logoManNotification.notification.id)
        }
        TelemetryWrapper.showHome()
        updateTopSitesData()
    }

    fun onPageBackground() {
        hasLoggedShowLogoman = false
    }

    fun onTopSitesPagePositionChanged(position: Int) {
        topSitesPageIndex.value = position
    }

    fun onBackgroundViewDoubleTap(): Boolean {
        // Not allowed double tap to switch theme when night mode is on
        if (settings.isNightModeEnable) return false

        toggleBackgroundColor.call()
        return true
    }

    fun onBackgroundViewLongPress() {
        // Not allowed long press to reset theme when night mode is on
        if (settings.isNightModeEnable) return

        resetBackgroundColor.call()
    }

    fun onShoppingButtonClicked() {
        openShoppingSearch.call()
        TelemetryWrapper.clickToolbarTabSwipe(TelemetryWrapper.Extra_Value.SHOPPING, TelemetryWrapper.Extra_Value.HOME)
    }

    fun onPrivateModeButtonClicked() {
        openPrivateMode.call()
        TelemetryWrapper.togglePrivateMode(true)
    }

    fun onTopSiteClicked(site: Site, position: Int) {
        when (site) {
            is Site.UrlSite -> {
                openBrowser.value = site.url
                val allowToLogTitle = when (site) {
                    is Site.UrlSite.FixedSite -> true
                    is Site.UrlSite.RemovableSite -> site.isDefault
                }
                val title = if (allowToLogTitle) site.title else ""
                val pageIndex = requireNotNull(topSitesPageIndex.value)
                val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
                val isAffiliate = site is Site.UrlSite.FixedSite
                TelemetryWrapper.clickTopSiteOn(topSitePosition, title, isAffiliate)
            }
        }
    }

    fun onTopSiteLongClicked(site: Site, position: Int): Boolean =
            if (site is Site.UrlSite.RemovableSite) {
                val pageIndex = requireNotNull(topSitesPageIndex.value)
                val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
                showTopSiteMenu.value = ShowTopSiteMenuData(site, topSitePosition)
                true
            } else {
                false
            }

    fun onPinTopSiteClicked(site: Site, position: Int) {
        when (site) {
            is Site.UrlSite -> {
                pinTopSiteUseCase(site)
                updateTopSitesData()
                val allowToLogTitle = if (site is Site.UrlSite.RemovableSite) {
                    site.isDefault
                } else {
                    false
                }
                val title = if (allowToLogTitle) site.title else ""
                val pageIndex = requireNotNull(topSitesPageIndex.value)
                val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
                TelemetryWrapper.pinTopSite(title, topSitePosition)
            }
        }
    }

    fun onRemoveTopSiteClicked(site: Site, position: Int) = viewModelScope.launch {
        when (site) {
            is Site.UrlSite.RemovableSite -> {
                removeTopSiteUseCase(site)
                updateTopSitesData()
                val allowToLogTitle = site.isDefault
                val title = if (allowToLogTitle) site.title else ""
                TelemetryWrapper.removeTopSite(site.isDefault, position, title)
            }
        }
    }

    fun onContentHubItemClicked(item: ContentHub.Item) = viewModelScope.launch {
        openContentPage.value = item
        readContentHubItemUseCase(item.getItemType())
        TelemetryWrapper.clickContentHub(item)
        val checkInResult = checkInMissionUseCase(
            when (item) {
                is ContentHub.Item.Travel -> CheckInMissionUseCase.PingType.Travel()
                is ContentHub.Item.Shopping -> CheckInMissionUseCase.PingType.Shopping()
                is ContentHub.Item.News -> CheckInMissionUseCase.PingType.Lifestyle()
                is ContentHub.Item.Games -> CheckInMissionUseCase.PingType.Game()
            }
        )
        checkInResult.data?.let { (mission, hasMissionCompleted) ->
            val (message, currentDay) = when (val progress = mission.missionProgress) {
                is MissionProgress.TypeDaily -> progress.message to progress.currentDay
                null -> error("Unknown MissionProgress type")
            }
            if (message.isNotEmpty()) {
                showToast.value = ToastMessage(message)
            }
            if (hasMissionCompleted) {
                showMissionCompleteDialog.value = mission
                TelemetryWrapper.showChallengeCompleteMessage()
            }
            TelemetryWrapper.endMissionTask(currentDay, hasMissionCompleted)
        }
    }

    fun onLogoManShown() {
        // Make it only animate once. Remove this when Home Screen doesn't recreate whenever goes back from browser
        val logoManNotification = logoManNotification.value
        logoManNotification?.animate = false
        if (!hasLoggedShowLogoman) {
            hasLoggedShowLogoman = true
            TelemetryWrapper.showLogoman(logoManType, logoManClickAction?.getLink(), logoManNotification?.notification?.id)
        }
    }

    fun onLogoManNotificationClicked() {
        logoManClickAction?.let {
            executeLogomanAction(it)
            val notification = logoManNotification.value?.notification
            if (notification != null && notification !is Notification.MissionNotification) {
                // no need to call dismissLogoManNotificationUseCase since it will also be done when mission detail page get opened
                dismissLogoManNotificationUseCase(notification)
            }
        }
        TelemetryWrapper.clickLogoman(logoManType, logoManClickAction?.getLink(), logoManNotification.value?.notification?.id)
    }

    private fun executeLogomanAction(logomanAction: GetLogoManNotificationUseCase.LogoManAction) {
        when (logomanAction) {
            is GetLogoManNotificationUseCase.LogoManAction.UriAction -> {
                // workaround to handle open url action then be able to back to home page when clicking back key
                if (logomanAction.action.startsWith("https://") or logomanAction.action.startsWith("http://")) {
                    openBrowser.value = logomanAction.action
                } else {
                    executeUriAction.value = logomanAction.action
                }
            }
            is GetLogoManNotificationUseCase.LogoManAction.OpenMissionPage -> openMissionDetailPage.value = logomanAction.mission
        }
    }

    fun onLogoManDismissed() {
        logoManNotification.value?.notification?.let {
            logoManNotification.value = null
            dismissLogoManNotificationUseCase(it)
            TelemetryWrapper.swipeLogoman(logoManType, logoManClickAction?.getLink(), it.id)
        }
    }

    fun onRewardButtonClicked() {
        TelemetryWrapper.clickRewardButton()
        openRewardPage.call()
    }

    fun onProfileButtonClicked() {
        openProfilePage.call()
    }

    fun onRedeemCompletedMissionButtonClicked(mission: Mission) {
        openMissionDetailPage.value = mission
        TelemetryWrapper.clickChallengeCompleteMessage(TelemetryWrapper.Extra_Value.LOGIN)
    }

    fun onContentHubRequestClickHintDismissed() {
        completeJoinMissionOnboardingUseCase()
    }

    fun onShowClickContentHubOnboarding() {
        TelemetryWrapper.showTaskContextualHint()
    }

    fun onRedeemCompletedLaterButtonClicked() {
        TelemetryWrapper.clickChallengeCompleteMessage(TelemetryWrapper.Extra_Value.LATER)
    }

    fun onRedeemCompletedDialogClosed() {
        TelemetryWrapper.clickChallengeCompleteMessage(TelemetryWrapper.Extra_Value.CLOSE)
    }

    fun onContentServiceOnboardingButtonClicked() {
        val timeSpent = System.currentTimeMillis() - contentServicesOnboardingTimeSpent
        if (isFirstRun) {
            TelemetryWrapper.clickFirstRunContextualHint("onboarding_2_content_services_news_shopping_games", timeSpent, 0, true)
        } else {
            TelemetryWrapper.clickWhatsnewContextualHint("onboarding_2_content_services_news_shopping_games", timeSpent, 0, true)
        }
        dismissContentServiceOnboardingDialog.call()
    }

    fun onNewTabButtonClicked() {
        showKeyboard.call()
    }

    data class ShowTopSiteMenuData(
        val site: Site,
        val position: Int
    )

    // Remove this when Home Screen doesn't recreate whenever goes back from browser
    data class StateNotification(
        val notification: Notification,
        var animate: Boolean
    )

    companion object {
        private const val TOP_SITES_MAX_PAGE_SIZE = 2
        private const val TOP_SITES_PER_PAGE = 8
    }
}

private fun GetLogoManNotificationUseCase.Notification.toUiModel() = when (this) {
    is GetLogoManNotificationUseCase.Notification.RemoteNotification -> Notification.RemoteNotification(id, title, subtitle, imageUrl)
    is GetLogoManNotificationUseCase.Notification.MissionNotification -> Notification.MissionNotification(id, title, subtitle, imageUrl)
}

private fun ContentHub.Item.getItemType() =
        when (this) {
            is ContentHub.Item.Travel -> ContentHubRepo.TRAVEL
            is ContentHub.Item.Shopping -> ContentHubRepo.SHOPPING
            is ContentHub.Item.News -> ContentHubRepo.NEWS
            is ContentHub.Item.Games -> ContentHubRepo.GAMES
        }