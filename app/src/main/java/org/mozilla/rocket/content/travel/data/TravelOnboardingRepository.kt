package org.mozilla.rocket.content.travel.data

import android.content.Context

class TravelOnboardingRepository(appContext: Context) {

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
        const val PREF_NAME = "travel"
        const val KEY_ONBOARDING = "travel_onboarding"
    }
}