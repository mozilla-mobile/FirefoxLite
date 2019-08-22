package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository.Site
import java.net.URLEncoder

class SearchShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(searchKeyword: String): Result<List<Site>> {
        val sites = repository.fetchSites()
        if (sites is Result.Success) {
            return Result.Success(sites.data.map { site ->
                Site(site.title, site.searchUrl + URLEncoder.encode(searchKeyword, "UTF-8"))
            })
        }
        return sites
    }
}