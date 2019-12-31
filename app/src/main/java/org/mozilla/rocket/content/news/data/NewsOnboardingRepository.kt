package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.strictmodeviolator.StrictModeViolation

class NewsOnboardingRepository(appContext: Context) {
    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    fun setOnboardingHasShown() {
        preference.edit().putBoolean(KEY_ONBOARDING, false).apply()
    }

    fun shouldShowOnboarding(): Boolean {
        return preference.getBoolean(KEY_ONBOARDING, true)
    }

    companion object {
        private const val PREF_NAME = "news"
        private const val KEY_ONBOARDING = "news_onboarding"
    }
}