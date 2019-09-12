package org.mozilla.rocket.shopping.search.data

import android.content.Context

class OnboardingSharedPreferenceRepository(appContext: Context) {

    private val preference = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getOnboardingPref(key: String): Boolean {
        return preference.getBoolean(key, true)
    }

    fun setOnboardingPref(key: String, showed: Boolean) {
        preference.run {
            edit().putBoolean(key, showed).apply()
        }
    }

    companion object {
        const val PREF_NAME = "shopping_search"
        const val KEY_CONTENT_SWITCH_ONBOARDING = "shopping_search_result_onboarding"
        const val KEY_ONBOARDING = "shopping_search_input_onboarding"
    }
}