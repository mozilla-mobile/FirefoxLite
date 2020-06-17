package org.mozilla.rocket.home.topsites.data

import org.mozilla.focus.history.model.Site

class RecommendedSitesResult(
    val categories: List<RecommendedSitesCategory>
)

data class RecommendedSitesCategory(
    val categoryId: String,
    val categoryName: String,
    val sites: List<Site>
)