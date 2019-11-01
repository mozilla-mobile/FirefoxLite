package org.mozilla.rocket.shopping.search.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import java.net.URLEncoder

class GetShoppingSearchSitesUseCase(val repository: ShoppingSearchRepository) {

    operator fun invoke(searchKeyword: String): LiveData<List<ShoppingSearchSite>> {
        repository.refreshShoppingSites()
        return repository.getShoppingSitesData()
            .map {
                it.filter { site -> site.isEnabled }
                    .map { site ->
                        ShoppingSearchSite(
                            site.title,
                            site.searchUrl + URLEncoder.encode(searchKeyword, "UTF-8"),
                            site.displayUrl
                        )
                    }
            }
    }

    data class ShoppingSearchSite(
        val title: String,
        val searchUrl: String,
        val displayUrl: String
    )
}