package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsDataSource
import org.mozilla.rocket.content.news.data.NewsSourceInfo
import org.mozilla.rocket.content.news.data.toJson
import org.mozilla.strictmodeviolator.StrictModeViolation

class DailyHuntSettingsLocalDataSource(private val appContext: Context) : NewsSettingsDataSource {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val newsLanguageList = ArrayList<NewsLanguage>()
            val jsonString = preference
                .getString(KEY_JSON_STRING_SUPPORT_LANGUAGES, "") ?: ""
            if (!TextUtils.isEmpty(jsonString)) {
                newsLanguageList.addAll(NewsLanguage.fromJson(jsonString))
            }
            Success(newsLanguageList)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setSupportLanguages(languages: List<NewsLanguage>) = withContext(Dispatchers.IO) {
        preference.edit().putString(KEY_JSON_STRING_SUPPORT_LANGUAGES, languages.toJson().toString()).apply()
    }

    override suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> = withContext(Dispatchers.IO) {
        return@withContext try {
            var selectedLanguage: NewsLanguage? = null
            val jsonString = preference
                .getString(KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE, "") ?: ""
            if (!TextUtils.isEmpty(jsonString)) {
                try {
                    val newsLanguageList = NewsLanguage.fromJson(jsonString)
                    if (newsLanguageList.isNotEmpty()) {
                        selectedLanguage = newsLanguageList[0].also { it.isSelected = true }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            Success(selectedLanguage)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setUserPreferenceLanguage(language: NewsLanguage) = withContext(Dispatchers.IO) {
        preference.edit().putString(KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE, language.toJson().toString()).apply()
    }

    override suspend fun getSupportCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val supportCategories = ArrayList<NewsCategory>()
            val jsonString = preference
                .getString(KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language, "") ?: ""
            if (!TextUtils.isEmpty(jsonString)) {
                supportCategories.addAll(NewsCategory.fromJson(jsonString))
                supportCategories.forEach { it.isSelected = true }
            }
            Success(supportCategories)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setSupportCategories(language: String, supportCategories: List<NewsCategory>) = withContext(Dispatchers.IO) {
        preference.edit().putString(
            KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language,
            supportCategories.toJson().toString()
        ).apply()
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = preference
                .getString(KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language, "") ?: ""
            val preferenceCategories = if (jsonString.isEmpty()) {
                emptyList()
            } else {
                NewsCategory.fromJson(jsonString)
            }
            Success(preferenceCategories)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) = withContext(Dispatchers.IO) {
        preference.edit().putString(
            KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language,
            userPreferenceCategories.toJson().toString()
        ).apply()
    }

    override fun getDefaultLanguage() = NewsLanguage("en", "en", "English")

    override fun getDefaultCategory() = NewsCategory("1", "News", 0, 1, true)

    override fun getAdditionalSourceInfo() = NewsSourceInfo(R.drawable.ic_dailyhunt_logo)

    override fun shouldEnableNewsSettings() = true

    override fun shouldEnablePersonalizedNews(): Boolean {
        throw UnsupportedOperationException("Can't get personalized news enable setting from device")
    }

    override fun hasUserEnabledPersonalizedNews(): Boolean {
        return preference.getBoolean(KEY_BOOL_IS_USER_ENABLED_PERSONALIZED_NEWS, false)
    }

    override fun setUserEnabledPersonalizedNews(enable: Boolean) {
        preference.edit().putBoolean(KEY_BOOL_IS_USER_ENABLED_PERSONALIZED_NEWS, enable).apply()
    }

    override fun shouldShowPersonalizedNewsOnboarding(): Boolean {
        return preference.getBoolean(KEY_BOOL_PERSONALIZED_NEWS_ONBOARDING, true)
    }

    override fun setPersonalizedNewsOnboardingHasShown() {
        preference.edit().putBoolean(KEY_BOOL_PERSONALIZED_NEWS_ONBOARDING, false).apply()
    }

    override fun shouldShowNewsLanguageSettingPage(): Boolean {
        return preference.getBoolean(KEY_BOOL_NEWS_LANGUAGE_SETTING_PAGE_STATE, true)
    }

    override fun setNewsLanguageSettingPageState(enable: Boolean) {
        preference.edit().putBoolean(KEY_BOOL_NEWS_LANGUAGE_SETTING_PAGE_STATE, enable).apply()
    }

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_JSON_STRING_SUPPORT_LANGUAGES = "dailyhunt_support_lang"
        private const val KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE = "dailyhunt_user_pref_lang"
        private const val KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX = "dailyhunt_support_cat_"
        private const val KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX = "dailyhunt_user_pref_cat_"
        private const val KEY_BOOL_IS_USER_ENABLED_PERSONALIZED_NEWS = "is_user_enabled_personalized_news"
        private const val KEY_BOOL_PERSONALIZED_NEWS_ONBOARDING = "personalized_news_onboarding"
        private const val KEY_BOOL_NEWS_LANGUAGE_SETTING_PAGE_STATE = "news_language_setting_page_state"
    }
}
