package org.mozilla.rocket.shopping.search.data

import android.content.Context
import org.json.JSONArray
import org.mozilla.strictmodeviolator.StrictModeViolation

class ShoppingSearchLocalDataSource(private val appContext: Context) : ShoppingSearchDataSource {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    private val mockPreferenceSiteList = listOf(
        ShoppingSite("Lazada", "https://www.lazada.co.id/catalog/?q=", "lazada.co.id", isEnabled = true),
        ShoppingSite("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=", "bukalapak.com", isEnabled = true),
        ShoppingSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=", "tokopedia.com", isEnabled = true),
        ShoppingSite("JD.ID", "https://m.jd.id/search?keywords=", "jd.id", isEnabled = true),
        ShoppingSite("Shopee", "https://shopee.co.id/search?keyword=", "shopee.co.id", isEnabled = true),
        ShoppingSite("BliBli", "https://www.blibli.com/jual/", "blibli.com", isEnabled = true)
    )

    override fun isShoppingSearchEnabled(): Boolean {
        throw UnsupportedOperationException("Always get shopping search settings from remote")
    }

    override fun getShoppingSites(): List<ShoppingSite> {
        preference.getString(KEY_SHOPPING_SEARCH_SITE, "")?.let {
            if (it.isNotEmpty()) {
                return it.toPreferenceSiteList()
            }
        }

        return mockPreferenceSiteList
    }

    override fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        val siteJsonArray = JSONArray()
        shoppingSites.map { it.toJson() }
            .forEach { siteJsonArray.put(it) }
        preference.edit().putString(KEY_SHOPPING_SEARCH_SITE, siteJsonArray.toString()).apply()
    }

    companion object {
        const val PREF_NAME = "shopping_search"
        const val KEY_SHOPPING_SEARCH_SITE = "shopping_search_site"
    }
}