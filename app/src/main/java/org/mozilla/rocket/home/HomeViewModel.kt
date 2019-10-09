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
import org.mozilla.rocket.home.onboarding.CheckFirstRunUseCase
import org.mozilla.rocket.home.onboarding.CheckLiteUpdate
import org.mozilla.rocket.home.onboarding.CompleteFirstRunUseCase
import org.mozilla.rocket.home.onboarding.CompleteLiteUpdate
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
    checkFirstRunUseCase: CheckFirstRunUseCase,
    completeFirstRunUseCase: CompleteFirstRunUseCase,
    checkLiteUpdate: CheckLiteUpdate,
    completeLiteUpdate: CompleteLiteUpdate,
    private val checkInMissionUseCase: CheckInMissionUseCase,
    private val completeJoinMissionOnboardingUseCase: CompleteJoinMissionOnboardingUseCase,
    getContentHubClickOnboardingEventUseCase: GetContentHubClickOnboardingEventUseCase,
    refreshMissionsUseCase: RefreshMissionsUseCase,
    hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
    getIsFxAccountUseCase: GetIsFxAccountUseCase
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
    val showOnboardingSpotlight = SingleLiveEvent<Unit>()
    val showToast = SingleLiveEvent<ToastMessage>()
    val openRewardPage = SingleLiveEvent<Unit>()
    val openProfilePage = SingleLiveEvent<Unit>()
    val showMissionCompleteDialog = SingleLiveEvent<Mission>()
    val openMissionDetailPage = SingleLiveEvent<Mission>()
    val showContentHubClickOnboarding = getContentHubClickOnboardingEventUseCase()

    private var logoManClickAction: GetLogoManNotificationUseCase.LogoManAction? = null

    init {
        initLogoManData()

        viewModelScope.launch {
            if (isMsrpAvailableUseCase()) {
                refreshMissionsUseCase()
            }
        }
        if (!checkFirstRunUseCase() || !checkLiteUpdate()) {
            completeFirstRunUseCase()
            completeLiteUpdate()
            showOnboardingSpotlight.call()
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
        TelemetryWrapper.clickTopSiteOn(position, title)
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
        TelemetryWrapper.pinTopsite(site.url, position)
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
            val message = when (val progress = mission.missionProgress) {
                is MissionProgress.TypeDaily -> progress.message
                null -> error("Unknown MissionProgress type")
            }
            if (message.isNotEmpty()) {
                showToast.value = ToastMessage(message)
            }
            if (hasMissionCompleted) {
                showMissionCompleteDialog.value = mission
            }
        }
    }

    fun onLogoManShown() {
        // Make it only animate once. Remove this when Home Screen doesn't recreate whenever goes back from browser
        logoManNotification.value?.animate = false
    }

    fun onLogoManNotificationClicked() {
        logoManClickAction?.let {
            when (it) {
                is GetLogoManNotificationUseCase.LogoManAction.OpenMissionPage -> openMissionDetailPage.value = it.mission
            }
        }
    }

    fun onLogoManDismissed() {
        logoManNotification.value?.let {
            dismissLogoManNotificationUseCase(it.notification)
        }
        logoManNotification.value = null
    }

    fun onRewardButtonClicked() {
        openRewardPage.call()
    }

    fun onProfileButtonClicked() {
        openProfilePage.call()
    }

    fun onRedeemCompletedMissionButtonClicked(mission: Mission) {
        openMissionDetailPage.value = mission
    }

    fun onContentHubRequestClickHintDismissed() {
        completeJoinMissionOnboardingUseCase()
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

private fun GetLogoManNotificationUseCase.Notification.toUiModel() = Notification(id, title, subtitle)