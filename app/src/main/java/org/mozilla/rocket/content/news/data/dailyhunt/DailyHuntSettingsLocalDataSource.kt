package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsDataSource
import org.mozilla.rocket.content.news.data.toJson

class DailyHuntSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val newsLanguageList = ArrayList<NewsLanguage>()
            val jsonString = getPreferences()
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
        getPreferences().edit().putString(KEY_JSON_STRING_SUPPORT_LANGUAGES, languages.toJson().toString()).apply()
    }

    override suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> = withContext(Dispatchers.IO) {
        return@withContext try {
            var selectedLanguage: NewsLanguage? = null
            val jsonString = getPreferences()
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
        getPreferences().edit().putString(KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE, language.toJson().toString()).apply()
    }

    override suspend fun getSupportCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val supportCategories = ArrayList<NewsCategory>()
            val jsonString = getPreferences()
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
        getPreferences().edit().putString(
            KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language,
            supportCategories.toJson().toString()
        ).apply()
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<NewsCategory>> {
        return Error(Exception("Not allow to customize category preferences"))
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) = Unit

    override fun getDefaultLanguage() = NewsLanguage("en", "en", "English")

    override fun getDefaultCategory() = NewsCategory("1", "News", 0, 1, true)

    override fun shouldEnableNewsSettings() = true

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_JSON_STRING_SUPPORT_LANGUAGES = "dailyhunt_support_lang"
        private const val KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE = "dailyhunt_user_pref_lang"
        private const val KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX = "dailyhunt_support_cat_"
    }
}
