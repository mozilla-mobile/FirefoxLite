package org.mozilla.rocket.shopping.search.data

import org.mozilla.focus.utils.FirebaseHelper

class ShoppingSearchRemoteDataSource : ShoppingSearchDataSource {

    override fun isShoppingSearchEnabled() =
        FirebaseHelper.getFirebase().getRcBoolean(RC_KEY_ENABLE_SHOPPING_SEARCH)

    override fun getShoppingSites(): List<ShoppingSite> {
        val shoppingSearchSites = FirebaseHelper.getFirebase().getRcString(RC_KEY_STR_SHOPPING_SEARCH_SITES)
        if (shoppingSearchSites.isNotEmpty()) {
            return shoppingSearchSites.toPreferenceSiteList()
        }

        return emptyList()
    }

    override fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        throw UnsupportedOperationException("Can't set user preference sites setting to server")
    }

    companion object {
        const val RC_KEY_ENABLE_SHOPPING_SEARCH = "enable_shopping_search"
        const val RC_KEY_STR_SHOPPING_SEARCH_SITES = "str_shopping_search_sites"
    }
}
