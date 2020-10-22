package org.mozilla.rocket.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.domain.ShouldShowNewMenuItemHintUseCase
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.domain.ReadContentHubItemUseCase
import org.mozilla.rocket.home.contenthub.domain.ShouldShowContentHubItemTextUseCase
import org.mozilla.rocket.home.contenthub.domain.ShouldShowContentHubUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.domain.IsHomeScreenShoppingButtonEnabledUseCase
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
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.rocket.util.ToastMessage

class HomeViewModel(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase,
    private val isTopSiteFullyPinnedUseCase: IsTopSiteFullyPinnedUseCase,
    private val pinTopSiteUseCase: PinTopSiteUseCase,
    private val removeTopSiteUseCase: RemoveTopSiteUseCase,
    getContentHubItemsUseCase: GetContentHubItemsUseCase,
    shouldShowContentHubItemTextUseCase: ShouldShowContentHubItemTextUseCase,
    private val readContentHubItemUseCase: ReadContentHubItemUseCase,
    private val isHomeScreenShoppingButtonEnabledUseCase: IsHomeScreenShoppingButtonEnabledUseCase,
    shouldShowShoppingSearchOnboardingUseCase: ShouldShowShoppingSearchOnboardingUseCase,
    setShoppingSearchOnboardingIsShownUseCase: SetShoppingSearchOnboardingIsShownUseCase,
    shouldShowNewMenuItemHintUseCase: ShouldShowNewMenuItemHintUseCase,
    shouldShowContentHubUseCase: ShouldShowContentHubUseCase,
    shouldShowThemeOnboardingUseCase: ShouldShowThemeOnboardingUseCase,
    setThemeOnboardingIsShownUseCase: SetThemeOnboardingIsShownUseCase,
    private val shouldShowSetDefaultBrowserOnboardingUseCase: ShouldShowSetDefaultBrowserOnboardingUseCase,
    private val setSetDefaultBrowserOnboardingIsShownUseCase: SetSetDefaultBrowserOnboardingIsShownUseCase
) : ViewModel(), TopSiteClickListener {

    val sitePages = MutableLiveData<List<SitePage>>()
    val topSitesPageIndex = MutableLiveData<Int>()
    val contentHubItems = getContentHubItemsUseCase()
    val shouldShowContentHubItemText = MutableLiveData<Boolean>().apply { value = shouldShowContentHubItemTextUseCase() }
    val isShoppingSearchEnabled = MutableLiveData<Boolean>().apply { value = isHomeScreenShoppingButtonEnabledUseCase() }
    val shouldShowNewMenuItemHint: LiveData<Boolean> = shouldShowNewMenuItemHintUseCase()
    val isContentHubEnabled: LiveData<Boolean> = shouldShowContentHubUseCase()

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val openShoppingSearch = SingleLiveEvent<Unit>()
    val openPrivateMode = SingleLiveEvent<Unit>()
    val openBrowser = SingleLiveEvent<String>()
    val showTopSiteMenu = SingleLiveEvent<ShowTopSiteMenuData>()
    val showAddTopSiteMenu = SingleLiveEvent<Unit>()
    val openContentPage = SingleLiveEvent<ContentHub.Item>()
    val showToast = SingleLiveEvent<ToastMessage>()
    val showShoppingSearchOnboardingSpotlight = SingleLiveEvent<Unit>()
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

    private var hasLoggedShowLogoman = false
    private var pinTopSiteResult: PinTopSiteUseCase.PinTopSiteResult? = null

    init {

        viewModelScope.launch {
        }
        if (shouldShowThemeOnboardingUseCase()) {
            setThemeOnboardingIsShownUseCase()
            showThemeSetting.call()
            TelemetryWrapper.showThemeContextualHint()
        } else if (shouldShowShoppingSearchOnboardingUseCase()) {
            setShoppingSearchOnboardingIsShownUseCase()
            showShoppingSearchOnboardingSpotlight.call()
            // To prevent showing in app message when onboarding
            FirebaseHelper.getFirebase().setIamMessagesSuppressed(true)
        }
    }

    fun onShoppingSearchOnboardingSpotlightDismiss() {
        FirebaseHelper.getFirebase().setIamMessagesSuppressed(false)
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
                val isAffiliate = site is Site.UrlSite.FixedSite
                TelemetryWrapper.clickTopSiteOn(topSitePosition, title, allowToLogTitle, isPinned, isAffiliate)
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
    }

    fun onNewTabButtonClicked() {
        showKeyboard.call()
    }

    data class ShowTopSiteMenuData(
        val site: Site,
        val position: Int
    )

    companion object {
        private const val TOP_SITES_MAX_PAGE_SIZE = 2
        private const val TOP_SITES_PER_PAGE = 8
    }
}

private fun ContentHub.Item.getItemType() =
        when (this) {
            is ContentHub.Item.Travel -> ContentHubRepo.TRAVEL
            is ContentHub.Item.Shopping -> ContentHubRepo.SHOPPING
            is ContentHub.Item.News -> ContentHubRepo.NEWS
            is ContentHub.Item.Games -> ContentHubRepo.GAMES
        }