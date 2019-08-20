package org.mozilla.rocket.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.TopSitesConfigsUseCase
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage

class HomeViewModel(
    private val settings: Settings,
    private val getTopSitesUseCase: GetTopSitesUseCase,
    topSitesConfigsUseCase: TopSitesConfigsUseCase,
    private val pinTopSiteUseCase: PinTopSiteUseCase,
    private val removeTopSiteUseCase: RemoveTopSiteUseCase,
    private val getContentHubItemsUseCase: GetContentHubItemsUseCase
) : ViewModel() {

    val sitePages = MutableLiveData<List<SitePage>>()
    val topSitesPageIndex = MutableLiveData<Int>()
    val pinEnabled = MutableLiveData<Boolean>().apply { value = topSitesConfigsUseCase().isPinEnabled }
    val contentHubItems = MutableLiveData<List<ContentHub.Item>>().apply { value = getContentHubItemsUseCase() }

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()
    val topSiteClicked = SingleLiveEvent<Site>()
    val topSiteLongClicked = SingleLiveEvent<Site>()
    val navigateToContentPage = SingleLiveEvent<ContentHub.Item>()

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
        // TODO:
    }

    fun onTopSiteClicked(site: Site, position: Int) {
        topSiteClicked.value = site
        val allowToLogTitle = when (site) {
            is Site.FixedSite -> true
            is Site.RemovableSite -> site.isDefault
        }
        val title = if (allowToLogTitle) site.title else ""
        TelemetryWrapper.clickTopSiteOn(position, title)
    }

    fun onTopSiteLongClicked(site: Site): Boolean =
            if (site is Site.RemovableSite) {
                topSiteLongClicked.value = site
                true
            } else {
                false
            }

    fun onPinTopSiteClicked(site: Site) {
        pinTopSiteUseCase(site)
        updateTopSitesData()
    }

    fun onRemoveTopSiteClicked(site: Site) = viewModelScope.launch {
        removeTopSiteUseCase(site)
        updateTopSitesData()
    }

    fun onContentHubItemClicked(item: ContentHub.Item) {
        navigateToContentPage.value = item
    }

    companion object {
        private const val TOP_SITES_MAX_PAGE_SIZE = 2
        private const val TOP_SITES_PER_PAGE = 8
    }
}