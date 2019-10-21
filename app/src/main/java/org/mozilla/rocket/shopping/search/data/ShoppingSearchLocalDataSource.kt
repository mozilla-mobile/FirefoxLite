package org.mozilla.rocket.shopping.search.data

import android.content.Context
import org.json.JSONArray
import org.mozilla.focus.utils.Settings
import org.mozilla.strictmodeviolator.StrictModeViolation

class ShoppingSearchLocalDataSource(private val appContext: Context) : ShoppingSearchDataSource {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    override fun isShoppingSearchEnabled(): Boolean {
        throw UnsupportedOperationException("Always get shopping search settings from remote")
    }

    override fun getShoppingSites(): List<ShoppingSite> {
        preference.getString(KEY_SHOPPING_SEARCH_SITE, "")?.let {
            if (it.isNotEmpty()) {
                return it.toPreferenceSiteList()
            }
        }

        return emptyList()
    }

    override fun getDefaultShoppingSites(): List<ShoppingSite> {
        return emptyList()
    }

    override fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        val siteJsonArray = JSONArray()
        shoppingSites.map { it.toJson() }
            .forEach { siteJsonArray.put(it) }
        preference.edit().putString(KEY_SHOPPING_SEARCH_SITE, siteJsonArray.toString()).apply()
    }

    override fun shouldEnableTurboMode(): Boolean =
        Settings.getInstance(appContext).shouldUseTurboMode()

    override fun shouldShowSearchInputOnboarding() =
        preference.getBoolean(KEY_SEARCH_INPUT_ONBOARDING, true)

    override fun setSearchInputOnboardingIsShown() =
        preference.edit().putBoolean(KEY_SEARCH_INPUT_ONBOARDING, false).apply()

    override fun shouldShowSearchResultOnboarding() =
        preference.getBoolean(KEY_SEARCH_RESULT_ONBOARDING, true)

    override fun setSearchResultOnboardingIsShown() =
        preference.edit().putBoolean(KEY_SEARCH_RESULT_ONBOARDING, false).apply()

    companion object {
        const val PREF_NAME = "shopping_search"
        const val KEY_SHOPPING_SEARCH_SITE = "shopping_search_site"
        const val KEY_SEARCH_INPUT_ONBOARDING = "shopping_search_input_onboarding"
        const val KEY_SEARCH_RESULT_ONBOARDING = "shopping_search_result_onboarding"
    }
}