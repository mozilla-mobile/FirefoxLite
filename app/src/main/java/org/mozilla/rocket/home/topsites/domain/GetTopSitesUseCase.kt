package org.mozilla.rocket.home.topsites.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel
import java.util.Locale

open class GetTopSitesUseCase(
    private val topSitesRepo: TopSitesRepo,
    private val contentPrefRepo: ContentPrefRepo
) {

    private val fixedSites: List<org.mozilla.focus.history.model.Site> by lazy {
        topSitesRepo.getConfiguredFixedSites() ?: topSitesRepo.getDefaultFixedSites() ?: emptyList()
    }

    open suspend operator fun invoke(): List<Site> = withContext(Dispatchers.IO) {
        val pinnedSites = topSitesRepo.getPinnedSites()
        val defaultSites = topSitesRepo.getChangedDefaultSites()
                ?: topSitesRepo.getConfiguredDefaultSiteGroups()?.find { it.groupId == contentPrefRepo.getContentPref().id }?.sites
                ?: topSitesRepo.getDefaultSites(contentPrefRepo.getContentPref().topSitesResId)
                ?: emptyList()
        val historySites = topSitesRepo.getHistorySites()

        composeTopSites(
            fixedSites,
            pinnedSites,
            defaultSites,
            historySites
        )
    }

    private fun composeTopSites(
        fixedSites: List<org.mozilla.focus.history.model.Site>,
        pinnedSites: List<org.mozilla.focus.history.model.Site>,
        defaultSites: List<org.mozilla.focus.history.model.Site>,
        historySites: List<org.mozilla.focus.history.model.Site>
    ): List<Site> {
        val result = fixedSites.toFixedSite() +
                pinnedSites.toRemovableSite(topSitesRepo) +
                mergeHistoryAndDefaultSites(defaultSites, historySites).toRemovableSite(topSitesRepo)

        return result.distinctBy { it.url.removeUrlPostSlash().toLowerCase(Locale.getDefault()) }
                .also { removeOutboundDefaultSites(it) }
                .take(TOP_SITES_SIZE)
    }

    private fun mergeHistoryAndDefaultSites(
        defaultSites: List<org.mozilla.focus.history.model.Site>,
        historySites: List<org.mozilla.focus.history.model.Site>
    ): List<org.mozilla.focus.history.model.Site> {
        val union = defaultSites + historySites
        val merged = union.groupBy { it.url.removeUrlPostSlash().toLowerCase(Locale.getDefault()) }
                .map {
                    val sameSiteGroup = it.value
                    if (sameSiteGroup.size == 1) {
                        sameSiteGroup.first()
                    } else {
                        var viewCount = 0L
                        var lastViewTimestamp = 0L
                        sameSiteGroup.forEach { site ->
                            viewCount += site.viewCount
                            if (site.lastViewTimestamp > lastViewTimestamp) {
                                lastViewTimestamp = site.lastViewTimestamp
                            }
                        }
                        // use default site if it exists
                        sameSiteGroup.first().apply {
                            setViewCount(viewCount)
                            setLastViewTimestamp(lastViewTimestamp)
                        }
                    }
                }

        return merged.sortedWith(
            compareBy<org.mozilla.focus.history.model.Site> { it.viewCount }.thenBy { it.lastViewTimestamp }
        ).reversed()
    }

    private fun removeOutboundDefaultSites(sites: List<Site>) {
        val sizeLimit = TOP_SITES_SIZE
        if (sites.size > sizeLimit) {
            val outboundSites = sites.takeLast(sites.size - sizeLimit)
            outboundSites.filter { it is Site.UrlSite.RemovableSite && it.isDefault }
                    .forEach { defaultSite ->
                        // Must be a RemovableSite
                        defaultSite as Site.UrlSite.RemovableSite
                        topSitesRepo.removeDefaultSite(defaultSite.toSiteModel(), contentPrefRepo.getContentPref().topSitesResId)
                    }
        }
    }

    companion object {
        private const val TOP_SITES_SIZE = 16
    }
}

fun String.removeUrlPostSlash(): String =
    if (isNotEmpty() && this[length - 1] == '/') {
        dropLast(1)
    } else {
        this
    }

fun List<org.mozilla.focus.history.model.Site>.toFixedSite(): List<Site.UrlSite> =
        map { it.toFixedSite() }

fun org.mozilla.focus.history.model.Site.toFixedSite(): Site.UrlSite =
        Site.UrlSite.FixedSite(
            id = id,
            title = title,
            url = url,
            iconUri = favIconUri,
            viewCount = viewCount,
            lastViewTimestamp = lastViewTimestamp
        )

private fun List<org.mozilla.focus.history.model.Site>.toRemovableSite(topSitesRepo: TopSitesRepo): List<Site.UrlSite> =
        map { it.toRemovableSite(topSitesRepo) }

private fun org.mozilla.focus.history.model.Site.toRemovableSite(topSitesRepo: TopSitesRepo): Site.UrlSite =
        Site.UrlSite.RemovableSite(
            id = id,
            title = title,
            url = url,
            iconUri = favIconUri,
            viewCount = viewCount,
            lastViewTimestamp = lastViewTimestamp,
            isDefault = isDefault,
            isPinned = topSitesRepo.isPinned(this)
        )