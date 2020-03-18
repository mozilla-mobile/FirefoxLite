package org.mozilla.rocket.shopping.search.data

interface ShoppingSearchDataSource {

    fun isShoppingSearchEnabled(): Boolean

    fun getShoppingSites(): List<ShoppingSite>

    fun getDefaultShoppingSites(): List<ShoppingSite>

    fun updateShoppingSites(shoppingSites: List<ShoppingSite>)

    fun shouldEnableTurboMode(): Boolean

    fun shouldShowSearchResultOnboarding(): Boolean

    fun setSearchResultOnboardingIsShown()

    fun getSearchPromptMessageShowCount(): Int

    fun setSearchPromptMessageShowCount(count: Int)

    fun getSearchDescription(): String

    fun getSearchLogoManImageUrl(): String
}