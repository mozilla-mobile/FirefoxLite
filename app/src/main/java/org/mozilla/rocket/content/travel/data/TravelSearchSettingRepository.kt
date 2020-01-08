package org.mozilla.rocket.content.travel.data

import android.content.Context
import org.mozilla.strictmodeviolator.StrictModeViolation

class TravelSearchSettingRepository(appContext: Context) {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    fun setSearchOptionPromptHasShown() {
        preference.edit().putBoolean(KEY_SEARCH_OPTION_PROMPT, false).apply()
    }

    fun shouldShowSearchOptionPrompt(): Boolean {
        return preference.getBoolean(KEY_SEARCH_OPTION_PROMPT, true)
    }

    fun setAsDefaultSearch(searchSetting: TravelSearchSetting) {
        val value = when (searchSetting) {
            TravelSearchSetting.Default -> VALUE_DEFAULT
            TravelSearchSetting.Google -> VALUE_GOOGLE
            TravelSearchSetting.FirefoxLite -> VALUE_FIREFOX_LITE
        }
        preference.edit().putInt(KEY_DEFAULT_SEARCH, value).apply()
    }

    fun getSearchSetting(): TravelSearchSetting {
        return when (preference.getInt(KEY_DEFAULT_SEARCH, VALUE_DEFAULT)) {
            VALUE_GOOGLE -> TravelSearchSetting.Google
            VALUE_FIREFOX_LITE -> TravelSearchSetting.FirefoxLite
            else -> TravelSearchSetting.Default
        }
    }

    sealed class TravelSearchSetting {
        object Default : TravelSearchSetting()
        object Google : TravelSearchSetting()
        object FirefoxLite : TravelSearchSetting()
    }

    companion object {
        private const val PREF_NAME = "travel"
        private const val KEY_SEARCH_OPTION_PROMPT = "search_option_prompt"
        private const val KEY_DEFAULT_SEARCH = "default_search"
        private const val VALUE_DEFAULT = 0
        private const val VALUE_GOOGLE = 1
        private const val VALUE_FIREFOX_LITE = 2
    }
}