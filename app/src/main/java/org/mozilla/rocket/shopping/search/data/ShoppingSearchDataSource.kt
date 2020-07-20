package org.mozilla.rocket.shopping.search.data

interface ShoppingSearchDataSource {

    fun getHomeShoppingSearchEnabledGroups(): List<HomeShoppingSearchEnabledGroup>?

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