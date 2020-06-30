package org.mozilla.rocket.home.topsites.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase.Companion.MAX_TOP_SITES_COUNT

class IsTopSiteFullyPinnedUseCase(private val topSitesRepo: TopSitesRepo) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val fixedSites = topSitesRepo.getConfiguredFixedSites() ?: emptyList()
        val pinnedSites = topSitesRepo.getPinnedSites()
        return@withContext (fixedSites.size + pinnedSites.size) >= MAX_TOP_SITES_COUNT
    }
}