package org.mozilla.rocket.home.topsites.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.toSiteModel

class PinTopSiteUseCase(private val topSitesRepo: TopSitesRepo) {

    suspend operator fun invoke(site: Site.UrlSite): PinTopSiteResult = withContext(Dispatchers.IO) {
        val fixedSites = topSitesRepo.getConfiguredFixedSites() ?: emptyList()
        val pinnedSites = topSitesRepo.getPinnedSites()
        val siteModel = site.toSiteModel()
        val existingIndex = (fixedSites + pinnedSites).indexOfFirst { pinnedSite -> pinnedSite.url.removeUrlPostSlash() == siteModel.url.removeUrlPostSlash() }
        return@withContext when {
            fixedSites.size + pinnedSites.size >= MAX_TOP_SITES_COUNT -> {
                PinTopSiteResult.FullyPinned
            }
            existingIndex != -1 -> {
                PinTopSiteResult.Existing(existingIndex)
            }
            else -> {
                topSitesRepo.pin(site.toSiteModel())
                PinTopSiteResult.Success(fixedSites.size + pinnedSites.size - 1)
            }
        }
    }

    sealed class PinTopSiteResult : Parcelable {
        @Parcelize
        class Success(val position: Int) : PinTopSiteResult()

        @Parcelize
        object FullyPinned : PinTopSiteResult()

        @Parcelize
        class Existing(val position: Int) : PinTopSiteResult()
    }

    companion object {
        const val MAX_TOP_SITES_COUNT = 16
    }
}