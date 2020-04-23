package org.mozilla.rocket.home.topsites.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel
import java.util.Locale

open class GetTopSitesAbTestingUseCase(private val topSitesRepo: TopSitesRepo) {

    private val sites: List<org.mozilla.focus.history.model.Site> by lazy {
        topSitesRepo.getAbTestingSites() ?: emptyList()
    }
    private val bucket: String? by lazy {
        LocalAbTesting.checkAssignedBucket(AB_TESTING_EXPERIMENT_NAME_TOP_SITES)
    }
    private val fixedSites: List<org.mozilla.focus.history.model.Site> by lazy {
        sites.take(getFixedSiteCount(bucket))
    }

    open suspend operator fun invoke(): List<Site> = withContext(Dispatchers.IO) {
        val pinnedSites = topSitesRepo.getPinnedSites()
        val defaultSites = topSitesRepo.getChangedDefaultSites()
                ?: sites.takeLast(sites.size - getFixedSiteCount(bucket) - getDefaultPinCount(bucket))
                        .apply { forEach { it.isDefault = true } }
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
        val result = fixedSites.toFixedSite() + pinnedSites.toPinnedSite() +
                mergeHistoryAndDefaultSites(defaultSites, historySites).toRemovableSite(topSitesRepo)

        return result.distinctBy { removeUrlPostSlash(it.url).toLowerCase(Locale.getDefault()) }
                .also { removeOutboundDefaultSites(it) }
                .take(TOP_SITES_SIZE)
    }

    private fun mergeHistoryAndDefaultSites(
        defaultSites: List<org.mozilla.focus.history.model.Site>,
        historySites: List<org.mozilla.focus.history.model.Site>
    ): List<org.mozilla.focus.history.model.Site> {
        val union = defaultSites + historySites
        val merged = union.groupBy { removeUrlPostSlash(it.url).toLowerCase(Locale.getDefault()) }
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

    private fun removeUrlPostSlash(url: String): String =
            if (url.isNotEmpty() && url[url.length - 1] == '/') {
                url.dropLast(1)
            } else {
                url
            }

    private fun removeOutboundDefaultSites(sites: List<Site>) {
        val sizeLimit = TOP_SITES_SIZE
        if (sites.size > sizeLimit) {
            val outboundSites = sites.takeLast(sites.size - sizeLimit)
            outboundSites.filter { it is Site.UrlSite.RemovableSite && it.isDefault }
                    .forEach { defaultSite ->
                        // Must be a RemovableSite
                        defaultSite as Site.UrlSite.RemovableSite
                        topSitesRepo.removeDefaultSiteAbTesting(defaultSite.toSiteModel())
                    }
        }
    }

    companion object {
        private const val TOP_SITES_SIZE = 16
        const val AB_TESTING_EXPERIMENT_NAME_TOP_SITES = "ab_testing_experiment_name_top_sites"
        // IN
        private const val AB_TESTING_IN_GROUP_CONTROL = "in_topsite_rr_0429_control"
        private const val AB_TESTING_IN_GROUP_AA = "in_topsite_rr_0429_aa"
        private const val AB_TESTING_IN_GROUP_1 = "in_topsite_rr_0429_2pinned"
        private const val AB_TESTING_IN_GROUP_2 = "in_topsite_rr_0429_2unpinable"
        private const val AB_TESTING_IN_GROUP_3 = "in_topsite_rr_0429_0pinned"
        private const val AB_TESTING_IN_GROUP_4 = "in_topsite_rr_0429_4unpinable"
        // ID
        private const val AB_TESTING_ID_GROUP_CONTROL = "id_topsite_rr_0429_control"
        private const val AB_TESTING_ID_GROUP_1 = "id_topsite_rr_0429_2pinned"
        private const val AB_TESTING_ID_GROUP_2 = "id_topsite_rr_0429_2unpinable"
        private const val AB_TESTING_ID_GROUP_3 = "id_topsite_rr_0429_0pinned"
        private const val AB_TESTING_ID_GROUP_4 = "id_topsite_rr_0429_4unpinable"

        fun getFixedSiteCount(bucket: String?) =
                when (bucket) {
                    // IN
                    AB_TESTING_IN_GROUP_CONTROL, AB_TESTING_IN_GROUP_AA -> 4
                    AB_TESTING_IN_GROUP_1 -> 2
                    AB_TESTING_IN_GROUP_2, AB_TESTING_IN_GROUP_3, AB_TESTING_IN_GROUP_4 -> 0
                    // ID
                    AB_TESTING_ID_GROUP_CONTROL -> 4
                    AB_TESTING_ID_GROUP_1 -> 2
                    AB_TESTING_ID_GROUP_2, AB_TESTING_ID_GROUP_3, AB_TESTING_ID_GROUP_4 -> 0
                    else -> error("Cannot find a corresponding user group: $bucket")
                }

        fun getDefaultPinCount(bucket: String?) =
                when (bucket) {
                    // IN
                    AB_TESTING_IN_GROUP_CONTROL, AB_TESTING_IN_GROUP_AA, AB_TESTING_IN_GROUP_1, AB_TESTING_IN_GROUP_3 -> 0
                    AB_TESTING_IN_GROUP_2 -> 2
                    AB_TESTING_IN_GROUP_4 -> 4
                    // ID
                    AB_TESTING_ID_GROUP_CONTROL, AB_TESTING_ID_GROUP_1, AB_TESTING_ID_GROUP_3 -> 0
                    AB_TESTING_ID_GROUP_2 -> 2
                    AB_TESTING_ID_GROUP_4 -> 4
                    else -> error("Cannot find a corresponding user group: $bucket")
                }
    }
}

private fun List<org.mozilla.focus.history.model.Site>.toFixedSite(): List<Site.UrlSite> =
        map { it.toFixedSite() }

private fun org.mozilla.focus.history.model.Site.toFixedSite(): Site.UrlSite =
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

private fun List<org.mozilla.focus.history.model.Site>.toPinnedSite(): List<Site.UrlSite> =
        map { it.toPinnedSite() }

private fun org.mozilla.focus.history.model.Site.toPinnedSite(): Site.UrlSite =
        Site.UrlSite.RemovableSite(
            id = id,
            title = title,
            url = url,
            iconUri = favIconUri,
            viewCount = viewCount,
            lastViewTimestamp = lastViewTimestamp,
            isDefault = isDefault,
            isPinned = true
        )