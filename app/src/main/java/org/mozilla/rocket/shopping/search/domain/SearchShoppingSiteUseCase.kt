package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository.Site

class SearchShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(searchKeyword: String): Result<List<Site>> {
        val sites = repository.fetchSites()
        if (sites is Result.Success) {
            sites.data.map { site ->
                Site(site.title, String.format(site.searchUrl, searchKeyword))
            }
        }
        return sites
    }
}