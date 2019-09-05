package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import java.net.URLEncoder

class GetShoppingSearchSitesUseCase(val repository: ShoppingSearchSiteRepository) {

    operator fun invoke(searchKeyword: String): List<ShoppingSearchSite> =
            repository.getShoppingSites().map {
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