package org.mozilla.rocket.shopping.search.data

interface ShoppingSearchDataSource {

    fun isShoppingSearchEnabled(): Boolean

    fun getShoppingSites(): List<ShoppingSite>

    fun updateShoppingSites(shoppingSites: List<ShoppingSite>)
}