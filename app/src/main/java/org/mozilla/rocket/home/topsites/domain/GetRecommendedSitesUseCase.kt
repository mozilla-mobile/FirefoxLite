package org.mozilla.rocket.home.topsites.domain

import org.mozilla.rocket.home.topsites.data.RecommendedSitesResult
import org.mozilla.rocket.home.topsites.data.TopSitesRepo

class GetRecommendedSitesUseCase(private val topSitesRepo: TopSitesRepo) {

    suspend operator fun invoke(): RecommendedSitesResult {
        return topSitesRepo.getConfiguredRecommendedSites()
            ?: topSitesRepo.getRecommendedSites()
            ?: RecommendedSitesResult(emptyList())
    }
}