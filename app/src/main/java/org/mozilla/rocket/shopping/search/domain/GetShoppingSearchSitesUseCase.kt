package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import java.net.URLEncoder

class GetShoppingSearchSitesUseCase(val repository: ShoppingSearchRepository) {

    operator fun invoke(searchKeyword: String): List<ShoppingSearchSite> =
            repository.getShoppingSites()
                .filter { site -> site.isEnabled }
                .map {
                ShoppingSearchSite(
                    title = it.title,
                    searchUrl = it.searchUrl + URLEncoder.encode(searchKeyword, "UTF-8")
                )
            }

    data class ShoppingSearchSite(
        val title: String,
        val searchUrl: String
    )
}