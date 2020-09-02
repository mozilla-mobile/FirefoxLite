package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.domain.ShouldShowNewMenuItemHintUseCase
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.first
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.domain.ReadContentHubItemUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.domain.IsHomeScreenShoppingButtonEnabledUseCase
import org.mozilla.rocket.home.logoman.domain.DismissLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.GetLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.LastReadLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification
import org.mozilla.rocket.home.onboarding.domain.SetSetDefaultBrowserOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.SetShoppingSearchOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.SetThemeOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowSetDefaultBrowserOnboardingUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowShoppingSearchOnboardingUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowThemeOnboardingUseCase
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.IsTopSiteFullyPinnedUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage
import org.mozilla.rocket.home.topsites.ui.TopSiteClickListener
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.domain.CheckInMissionUseCase
import org.mozilla.rocket.msrp.domain.CompleteJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.IsMsrpAvailableUseCase
import org.mozilla.rocket.msrp.domain.LastReadMissionIdUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.rocket.util.ToastMessage

class HomeViewModel(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase,
    private val isTopSiteFullyPinnedUseCase: IsTopSiteFullyPinnedUseCase,
    private val pinTopSiteUseCase: PinTopSiteUseCase,
    private val removeTopSiteUseCase: RemoveTopSiteUseCase,
    getContentHubItemsUseCase: GetContentHubItemsUseCase,
    private val readContentHubItemUseCase: ReadContentHubItemUseCase,
    private val getLogoManNotificationUseCase: GetLogoManNotificationUseCase,
    private val lastReadLogoManNotificationUseCase: LastReadLogoManNotificationUseCase,
    private val lastReadMissionIdUseCase: LastReadMissionIdUseCase,
    private val dismissLogoManNotificationUseCase: DismissLogoManNotificationUseCase,
    private val isMsrpAvailableUseCase: IsMsrpAvailableUseCase,
    private val isHomeScreenShoppingButtonEnabledUseCase: IsHomeScreenShoppingButtonEnabledUseCase,
    private val checkInMissionUseCase: CheckInMissionUseCase,
    private val completeJoinMissionOnboardingUseCase: CompleteJoinMissionOnboardingUseCase,
    refreshMissionsUseCase: RefreshMissionsUseCase,
    hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
    getIsFxAccountUseCase: GetIsFxAccountUseCase,
    shouldShowShoppingSearchOnboardingUseCase: ShouldShowShoppingSearchOnboardingUseCase,
    setShoppingSearchOnboardingIsShownUseCase: SetShoppingSearchOnboardingIsShownUseCase,
    shouldShowNewMenuItemHintUseCase: ShouldShowNewMenuItemHintUseCase,
    shouldShowThemeOnboardingUseCase: ShouldShowThemeOnboardingUseCase,
    setThemeOnboardingIsShownUseCase: SetThemeOnboardingIsShownUseCase,
    private val shouldShowSetDefaultBrowserOnboardingUseCase: ShouldShowSetDefaultBrowserOnboardingUseCase,
    private val setSetDefaultBrowserOnboardingIsShownUseCase: SetSetDefaultBrowserOnboardingIsShownUseCase
) : ViewModel(), TopSiteClickListener {

    val sitePages = MutableLiveData<List<SitePage>>()
    val topSitesPageIndex = MutableLiveData<Int>()
    val contentHubItems = getContentHubItemsUseCase()
    val logoManNotification = MediatorLiveData<StateNotification?>()
    val isAccountLayerVisible = MutableLiveData<Boolean>().apply { value = isMsrpAvailableUseCase() }
    val isShoppingSearchEnabled = MutableLiveData<Boolean>().apply { value = isHomeScreenShoppingButtonEnabledUseCase() }
    val hasUnreadMissions: LiveData<Boolean> = hasUnreadMissionsUseCase()
    val isFxAccount: LiveData<Boolean> = getIsFxAccountUseCase()
    val shouldShowNewMenuItemHint: LiveData<Boolean> = shouldShowNewMenuItemHintUseCase()

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val openShoppingSearch = SingleLiveEvent<Unit>()
    val openPrivateMode = SingleLiveEvent<Unit>()
    val openBrowser = SingleLiveEvent<String>()
    val showTopSiteMenu = SingleLiveEvent<ShowTopSiteMenuData>()
    val showAddTopSiteMenu = SingleLiveEvent<Unit>()
    val openContentPage = SingleLiveEvent<ContentHub.Item>()
    val showToast = SingleLiveEvent<ToastMessage>()
    val openRewardPage = SingleLiveEvent<Unit>()
    val openProfilePage = SingleLiveEvent<Unit>()
    val showMissionCompleteDialog = SingleLiveEvent<Mission>()
    val openMissionDetailPage = SingleLiveEvent<Mission>()
    val showShoppingSearchOnboardingSpotlight = SingleLiveEvent<Unit>()
    val hideLogoManNotification = SingleLiveEvent<Unit>()
    val executeUriAction = SingleLiveEvent<String>()
    val showKeyboard = SingleLiveEvent<Unit>()
    val openAddNewTopSitesPage = SingleLiveEvent<Unit>()
    val addNewTopSiteSuccess = SingleLiveEvent<Int>()
    val addNewTopSiteFullyPinned = SingleLiveEvent<Unit>()
    val addExistingTopSite = SingleLiveEvent<Int>()
    val homeBackgroundColorThemeClicked = SingleLiveEvent<ThemeManager.ThemeSet>()
    val showThemeSetting = SingleLiveEvent<Unit>()
    val showSetAsDefaultBrowserOnboarding = SingleLiveEvent<Unit>()
    val tryToSetDefaultBrowser = SingleLiveEvent<Unit>()

    private var logoManClickAction: GetLogoManNotificationUseCase.LogoManAction? = null
    private var logoManType: String? = null
    private var hasLoggedShowLogoman = false
    private var pinTopSiteResult: PinTopSiteUseCase.PinTopSiteResult? = null

    init {
        initLogoManData()

        viewModelScope.launch {
            if (isMsrpAvailableUseCase()) {
                refreshMissionsUseCase()
            }
        }
        if (shouldShowThemeOnboardingUseCase()) {
            setThemeOnboardingIsShownUseCase()
            showThemeSetting.call()
            TelemetryWrapper.showThemeContextualHint()
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
        val topSiteList = getTopSitesUseCase()
        sitePages.value = if (topSiteList.isNotEmpty()) {
            topSiteList.addDummyTopSites().toSitePages().also { sitePages ->
                val sitePosition = when (val result = pinTopSiteResult) {
                    is PinTopSiteUseCase.PinTopSiteResult.Success -> result.position
                    is PinTopSiteUseCase.PinTopSiteResult.Existing -> result.position
                    else -> -1
                }
                if (sitePosition != -1) {
                    val sitePage = sitePosition / TOP_SITES_PER_PAGE
                    val siteInPageIndex = sitePosition % TOP_SITES_PER_PAGE
                    if (sitePage < sitePages.size && siteInPageIndex < sitePages[sitePage].sites.size) {
                        when (val topSite = sitePages[sitePage].sites[siteInPageIndex]) {
                            is Site.UrlSite -> topSite.highlight = true
                        }
                    }
                }
                pinTopSiteResult = null
            }
        } else {
            listOf(Site.EmptyHintSite).addDummyTopSites().toSitePages()
        }
    }

    private fun List<Site>.toSitePages(): List<SitePage> = chunked(TOP_SITES_PER_PAGE)
            .take(TOP_SITES_MAX_PAGE_SIZE)
            .map { SitePage(it) }

    private fun List<Site>.addDummyTopSites(): List<Site> {
        val siteListWithDummySite = this.toMutableList()
        val dummySiteSize = TOP_SITES_PER_PAGE * TOP_SITES_MAX_PAGE_SIZE - this.size
        for (i in 0 until dummySiteSize) {
            siteListWithDummySite.add(Site.DummySite)
        }
        return siteListWithDummySite
    }

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
        // Not allowed double tap to switch theme when dark theme is on
        if (settings.isDarkThemeEnable) return false

        toggleBackgroundColor.call()
        return true
    }

    fun onBackgroundViewLongPress() {
        // Not allowed long press to reset theme when dark theme is on
        if (settings.isDarkThemeEnable) return

        resetBackgroundColor.call()
    }

    fun onThemeClicked(isDarkTheme: Boolean, theme: ThemeManager.ThemeSet) {
        settings.setDarkTheme(isDarkTheme)
        // No need to update background color theme when in dark theme
        if (isDarkTheme) {
            return
        }
        homeBackgroundColorThemeClicked.value = theme
    }

    fun onShoppingButtonClicked() {
        openShoppingSearch.call()
        TelemetryWrapper.clickToolbarTabSwipe(TelemetryWrapper.Extra_Value.SHOPPING, TelemetryWrapper.Extra_Value.HOME)
    }

    fun onPrivateModeButtonClicked() {
        openPrivateMode.call()
        TelemetryWrapper.togglePrivateMode(true)
    }

    override fun onTopSiteClicked(site: Site, position: Int) {
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
                val isPinned = if (site is Site.UrlSite.RemovableSite) {
                    site.isPinned
                } else {
                    false
                }
                TelemetryWrapper.clickTopSiteOn(topSitePosition, title, allowToLogTitle, isPinned)
            }
            is Site.EmptyHintSite -> {
                openAddNewTopSitesPage()
                TelemetryWrapper.addTopSite(TelemetryWrapper.Extra_Value.EMPTY_HINT)
            }
        }
    }

    override fun onTopSiteLongClicked(site: Site, position: Int): Boolean =
            if (site is Site.UrlSite.RemovableSite || site is Site.DummySite) {
                val pageIndex = requireNotNull(topSitesPageIndex.value)
                val topSitePosition = position + pageIndex * TOP_SITES_PER_PAGE
                when (site) {
                    is Site.UrlSite.RemovableSite -> showTopSiteMenu.value = ShowTopSiteMenuData(site, topSitePosition)
                    is Site.DummySite -> showAddTopSiteMenu.call()
                }
                true
            } else {
                false
            }

    fun onPinTopSiteClicked(site: Site, position: Int) = viewModelScope.launch {
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
                TelemetryWrapper.pinTopSite(title, position, allowToLogTitle)
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
                TelemetryWrapper.removeTopSite(site.isDefault, position, title, site.isPinned)
            }
        }
    }

    fun onAddNewTopSiteResult(pinTopSiteResult: PinTopSiteUseCase.PinTopSiteResult) {
        this.pinTopSiteResult = pinTopSiteResult
        when (pinTopSiteResult) {
            is PinTopSiteUseCase.PinTopSiteResult.Success -> {
                addNewTopSiteSuccess.value = pinTopSiteResult.position / TOP_SITES_PER_PAGE
            }
            is PinTopSiteUseCase.PinTopSiteResult.Existing -> {
                addExistingTopSite.value = pinTopSiteResult.position / TOP_SITES_PER_PAGE
            }
        }
    }

    fun onAddTopSiteContextMenuClicked() {
        openAddNewTopSitesPage()
        TelemetryWrapper.addTopSite(TelemetryWrapper.Extra_Value.CONTEXT_MENU)
    }

    private fun openAddNewTopSitesPage() = viewModelScope.launch {
        val fullyPinned = isTopSiteFullyPinnedUseCase()
        if (fullyPinned) {
            addNewTopSiteFullyPinned.call()
        } else {
            openAddNewTopSitesPage.call()
        }
    }

    fun onAddTopSiteMenuClicked() {
        openAddNewTopSitesPage()
    }

    fun onAddMoreTopSiteSnackBarClicked() {
        openAddNewTopSitesPage()
        TelemetryWrapper.clickAddTopSiteFromSnackBar()
    }

    fun onThemeSettingMenuClicked() {
        showThemeSetting.call()
    }

    fun onExitThemeSetting(selectedTheme: String) {
        if (shouldShowSetDefaultBrowserOnboardingUseCase()) {
            setSetDefaultBrowserOnboardingIsShownUseCase()
            showSetAsDefaultBrowserOnboarding.call()
            TelemetryWrapper.showGoSetDefaultMessage()
        }
        TelemetryWrapper.clickThemeContextualHint(selectedTheme)
    }

    fun onSetAsDefaultBrowserClicked() {
        tryToSetDefaultBrowser.call()
        TelemetryWrapper.clickGoSetDefaultMessage(TelemetryWrapper.Extra_Value.OK)
    }

    fun onCancelSetAsDefaultBrowserClicked() {
        TelemetryWrapper.clickGoSetDefaultMessage(TelemetryWrapper.Extra_Value.LATER)
    }

    fun onClearBrowsingHistory() {
        updateTopSitesData()
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