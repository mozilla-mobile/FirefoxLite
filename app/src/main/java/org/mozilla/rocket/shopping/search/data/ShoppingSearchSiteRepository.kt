package org.mozilla.rocket.shopping.search.data

import org.mozilla.rocket.content.Result

class ShoppingSearchSiteRepository {

    suspend fun fetchSites(): Result<List<Site>> {
        return Result.Success(listOf(
            Site("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D="),
            Site("Tokopedia", "https://www.tokopedia.com/search?st=product&q="),
            Site("JD.ID", "https://www.jd.id/search?keywords=")
        ))
    }

    data class Site(
        val title: String,
        val searchUrl: String
    )
}