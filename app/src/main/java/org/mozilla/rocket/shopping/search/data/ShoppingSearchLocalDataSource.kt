package org.mozilla.rocket.shopping.search.data

import android.content.Context
import org.json.JSONArray
import org.mozilla.focus.R
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.abtesting.LocalAbTesting
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

    override fun shouldShowSearchResultOnboarding() =
        preference.getBoolean(KEY_SEARCH_RESULT_ONBOARDING, true)

    override fun setSearchResultOnboardingIsShown() =
        preference.edit().putBoolean(KEY_SEARCH_RESULT_ONBOARDING, false).apply()

    override fun getSearchPromptMessageShowCount() =
        preference.getInt(KEY_SEARCH_PROMPT_MESSAGE_SHOW_COUNT, 0)

    override fun setSearchPromptMessageShowCount(count: Int) =
        preference.edit().putInt(KEY_SEARCH_PROMPT_MESSAGE_SHOW_COUNT, count).apply()

    override fun getSearchDescription(): String {
        return if (LocalAbTesting.checkAssignedBucket(SMART_SHOPPING_COPY_AB_TESTING)
                == SMART_SHOPPING_COPY_B) {
            appContext.getString(R.string.shopping_search_onboarding_body_B)
        } else {
            appContext.getString(R.string.shopping_search_onboarding_body_A)
        }
    }

    override fun getSearchLogoManImageUrl(): String {
        throw UnsupportedOperationException("Always get shopping search logo man url from remote")
    }

    companion object {
        const val PREF_NAME = "shopping_search"
        const val KEY_SHOPPING_SEARCH_SITE = "shopping_search_site"
        const val KEY_SEARCH_RESULT_ONBOARDING = "shopping_search_result_onboarding"
        const val KEY_SEARCH_PROMPT_MESSAGE_SHOW_COUNT = "shopping_search_prompt_message_show_count"

        private const val SMART_SHOPPING_COPY_AB_TESTING = "smart_shopping_copy_ab_testing"
        private const val SMART_SHOPPING_COPY_B = "smart_shopping_copy_b"
    }
}