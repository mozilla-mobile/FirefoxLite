package org.mozilla.rocket.shopping.search.data

import org.mozilla.focus.utils.FirebaseHelper

class ShoppingSearchRemoteDataSource : ShoppingSearchDataSource {

    private val defaultShoppingSiteListJson = "[{\"title\":\"Google\",\"searchUrl\":\"https://www.google.com/search?q=\",\"displayUrl\":\"google.com\",\"showPrompt\":false},{\"title\":\"Amazon\",\"searchUrl\":\"https://www.amazon.com/s?k=\",\"displayUrl\":\"amazon.com\"},{\"title\":\"eBay\",\"searchUrl\":\"https://www.ebay.com/sch/i.html?_nkw=\",\"displayUrl\":\"ebay.com\"},{\"title\":\"Aliexpress\",\"searchUrl\":\"https://www.aliexpress.com/wholesale?SearchText=\",\"displayUrl\":\"aliexpress.com\"}]"

    override fun isShoppingSearchEnabled() =
        FirebaseHelper.getFirebase().getRcBoolean(RC_KEY_ENABLE_SHOPPING_SEARCH)

    override fun getShoppingSites(): List<ShoppingSite> {
        val shoppingSearchSites = FirebaseHelper.getFirebase().getRcString(RC_KEY_STR_SHOPPING_SEARCH_SITES)
        if (shoppingSearchSites.isNotEmpty()) {
            return shoppingSearchSites.toPreferenceSiteList()
        }

        return emptyList()
    }

    override fun getDefaultShoppingSites(): List<ShoppingSite> {
        return defaultShoppingSiteListJson.toPreferenceSiteList()
    }

    override fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        throw UnsupportedOperationException("Can't set user preference sites setting to server")
    }

    override fun shouldEnableTurboMode(): Boolean {
        throw UnsupportedOperationException("Can't get turbo mode setting from server")
    }

    override fun shouldShowSearchInputOnboarding(): Boolean {
        throw UnsupportedOperationException("Can't get search input onboarding status from server")
    }

    override fun setSearchInputOnboardingIsShown() {
        throw UnsupportedOperationException("Can't set search input onboarding status to server")
    }

    override fun shouldShowSearchResultOnboarding(): Boolean {
        throw UnsupportedOperationException("Can't get search result onboarding status from server")
    }

    override fun setSearchResultOnboardingIsShown() {
        throw UnsupportedOperationException("Can't set search result onboarding status to server")
    }

    companion object {
        const val RC_KEY_ENABLE_SHOPPING_SEARCH = "enable_shopping_search"
        const val RC_KEY_STR_SHOPPING_SEARCH_SITES = "str_shopping_search_sites"
    }
}
