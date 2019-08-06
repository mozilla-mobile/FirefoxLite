package org.mozilla.rocket.content.news.data

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.Result.Success
import java.util.Locale

class NewsSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_JSON_STRING_SUPPORT_LANGUAGES = "support_lang"
        private const val KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE = "user_pref_lang"
        private const val KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX = "support_cat_"
        private const val KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX = "user_pref_cat_"
        private const val DEFAULT_LANGUAGE_KEY = "English"
        private const val DEFAULT_LANGUAGE_CODE = "1"
        private const val DEFAULT_CATEGORY_ID = "top-news"
        private val DEFAULT_CATEGORY_LIST = listOf(
            DEFAULT_CATEGORY_ID
        )
    }

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
            var selectedLanguage = getDefaultPreferenceLanguage()
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

    override suspend fun getSupportCategories(language: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language, "") ?: ""
            Success(toCategoryList(jsonString))
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setSupportCategories(language: String, supportCategories: List<String>) = withContext(Dispatchers.IO) {
        getPreferences().edit().putString(
            KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language,
            categoryListToJsonArray(supportCategories).toString()
        ).apply()
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language, "") ?: ""
            val preferenceCategories = if (jsonString.isEmpty()) {
                emptyList()
            } else {
                toCategoryList(jsonString)
            }
            Success(preferenceCategories)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) = withContext(Dispatchers.IO) {
        getPreferences().edit().putString(
            KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language,
            categoryListToJsonArray(userPreferenceCategories).toString()
        ).apply()
    }

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun getDefaultPreferenceLanguage(): NewsLanguage {
        var defaultLanguage = NewsLanguage(DEFAULT_LANGUAGE_KEY, DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_KEY)
        val jsonString = getPreferences()
            .getString(KEY_JSON_STRING_SUPPORT_LANGUAGES, "") ?: ""
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                val newsLanguageResult = NewsLanguage.fromJson(jsonString)
                newsLanguageResult
                    .find { language -> Locale.getDefault().displayName.contains(language.name) }
                    ?.let { defaultLanguage = it }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return defaultLanguage.also { it.isSelected = true }
    }

    private fun categoryListToJsonArray(categories: List<String>): JSONArray {
        val jsonArray = JSONArray()
        for (category in categories) {
            jsonArray.put(category)
        }

        return jsonArray
    }

    private fun toCategoryList(jsonString: String): List<String> {
        val result = ArrayList<String>()
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                val items = JSONArray(jsonString)
                for (i in 0 until items.length()) {
                    val categoryId = items.optString(i)
                    result.add(categoryId)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return if (result.size > 0) {
            result
        } else {
            DEFAULT_CATEGORY_LIST
        }
    }
}
