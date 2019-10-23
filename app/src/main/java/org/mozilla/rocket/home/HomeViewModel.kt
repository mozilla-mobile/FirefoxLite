package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.first
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.domain.IsShoppingButtonEnabledUseCase
import org.mozilla.rocket.home.logoman.domain.DismissLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.GetLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification
import org.mozilla.rocket.home.onboarding.CompleteHomeOnboardingUseCase
import org.mozilla.rocket.home.onboarding.IsNeedToShowHomeOnboardingUseCase
import org.mozilla.rocket.home.onboarding.domain.IsNewUserUseCase
import org.mozilla.rocket.home.onboarding.domain.SetShoppingSearchOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowShoppingSearchOnboardingUseCase
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
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.util.ToastMessage

class HomeViewModel(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase,
    topSitesConfigsUseCase: TopSitesConfigsUseCase,
    private val pinTopSiteUseCase: PinTopSiteUseCase,
    private val removeTopSiteUseCase: RemoveTopSiteUseCase,
    private val getContentHubItemsUseCase: GetContentHubItemsUseCase,
    private val getLogoManNotificationUseCase: GetLogoManNotificationUseCase,
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
    private val isNewUserUseCase: IsNewUserUseCase
) : ViewModel() {

    val sitePages = MutableLiveData<List<SitePage>>()
    val topSitesPageIndex = MutableLiveData<Int>()
    val pinEnabled = MutableLiveData<Boolean>().apply { value = topSitesConfigsUseCase().isPinEnabled }
    val contentHubItems = MutableLiveData<List<ContentHub.Item>>().apply { value = getContentHubItemsUseCase() }
    val logoManNotification = MediatorLiveData<StateNotification?>()
    val isAccountLayerVisible = MutableLiveData<Boolean>().apply { value = isMsrpAvailableUseCase() }
    val isShoppingSearchEnabled = MutableLiveData<Boolean>().apply { value = isShoppingButtonEnabledUseCase() }
    val hasUnreadMissions: LiveData<Boolean> = hasUnreadMissionsUseCase()
    val isFxAccount: LiveData<Boolean> = getIsFxAccountUseCase()

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val openShoppingSearch = SingleLiveEvent<Unit>()
    val openPrivateMode = SingleLiveEvent<Unit>()
    val openBrowser = SingleLiveEvent<Site>()
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

    private var logoManClickAction: GetLogoManNotificationUseCase.LogoManAction? = null
    private var contentServicesOnboardingTimeSpent = 0L

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
            if (isNewUserUseCase()) {
                TelemetryWrapper.showFirstRunContextualHint("onboarding_2_content_services_news_shopping_games")
            } else {
                TelemetryWrapper.showWhatsnewContextualHint("onboarding_2_content_services_news_shopping_games")
            }
            showContentServicesOnboardingSpotlight.call()
        } else if (shouldShowShoppingSearchOnboardingUseCase()) {
            setShoppingSearchOnboardingIsShownUseCase()
            showShoppingSearchOnboardingSpotlight.call()
        }
    }

    private fun initLogoManData() {
        logoManNotification.addSource(
                getLogoManNotificationUseCase().first()
                        .map {
                            logoManClickAction = it?.action
                            it?.run { StateNotification(it.toUiModel(), true) }
                        }
        ) {
            logoManNotification.value = it
        }
    }

    fun updateTopSitesData() = viewModelScope.launch {
        sitePages.value = getTopSitesUseCase().toSitePages()
    }

    private fun List<Site>.toSitePages(): List<SitePage> = chunked(TOP_SITES_PER_PAGE)
            .filterIndexed { index, _ -> index < TOP_SITES_MAX_PAGE_SIZE }
            .map { SitePage(it) }

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
        openBrowser.value = site
        val allowToLogTitle = when (site) {
            is Site.FixedSite -> true
            is Site.RemovableSite -> site.isDefault
        }
        val title = if (allowToLogTitle) site.title else ""
        val pageIndex = requireNotNull(topSitesPageIndex.value)
        val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
        val isAffiliate = site is Site.FixedSite
        TelemetryWrapper.clickTopSiteOn(topSitePosition, title, isAffiliate)
    }

    fun onTopSiteLongClicked(site: Site, position: Int): Boolean =
            if (site is Site.RemovableSite) {
                val pageIndex = requireNotNull(topSitesPageIndex.value)
                val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
                showTopSiteMenu.value = ShowTopSiteMenuData(site, topSitePosition)
                true
            } else {
                false
            }

    fun onPinTopSiteClicked(site: Site, position: Int) {
        pinTopSiteUseCase(site)
        updateTopSitesData()
        val pageIndex = requireNotNull(topSitesPageIndex.value)
        val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
        TelemetryWrapper.pinTopSite(site.title, topSitePosition)
    }

    fun onRemoveTopSiteClicked(site: Site) = viewModelScope.launch {
        site as Site.RemovableSite
        removeTopSiteUseCase(site)
        updateTopSitesData()
        TelemetryWrapper.removeTopSite(site.isDefault)
    }

    fun onContentHubItemClicked(item: ContentHub.Item) = viewModelScope.launch {
        openContentPage.value = item
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
        logoManNotification.value?.animate = false
        TelemetryWrapper.showLogoman(TelemetryWrapper.Extra_Value.REWARDS, null)
    }

    fun onLogoManNotificationClicked() {
        logoManClickAction?.let {
            when (it) {
                is GetLogoManNotificationUseCase.LogoManAction.OpenMissionPage -> openMissionDetailPage.value = it.mission
            }
        }
        TelemetryWrapper.clickLogoman(TelemetryWrapper.Extra_Value.REWARDS, null)
    }

    fun onLogoManDismissed() {
        logoManNotification.value?.notification?.let {
            dismissLogoManNotificationUseCase(it)
        }
        logoManNotification.value = null
        TelemetryWrapper.swipeLogoman(TelemetryWrapper.Extra_Value.REWARDS, null)
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
        if (isNewUserUseCase()) {
            TelemetryWrapper.clickFirstRunContextualHint("onboarding_2_content_services_news_shopping_games", timeSpent, 0, true)
        } else {
            TelemetryWrapper.clickWhatsnewContextualHint("onboarding_2_content_services_news_shopping_games", timeSpent, 0, true)
        }
        dismissContentServiceOnboardingDialog.call()
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