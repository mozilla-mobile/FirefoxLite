package org.mozilla.rocket.shopping.search.data

import org.mozilla.focus.utils.FirebaseHelper

class ShoppingSearchRemoteDataSource : ShoppingSearchDataSource {

    override fun isShoppingSearchEnabled() =
        FirebaseHelper.getFirebase().getRcBoolean(RC_KEY_ENABLE_SHOPPING_SEARCH)

    override fun getShoppingSites(): List<ShoppingSite> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        throw UnsupportedOperationException("Can't set user preference sites setting to server")
    }

    companion object {
        const val RC_KEY_ENABLE_SHOPPING_SEARCH = "key_enable_shopping_search"
    }
}