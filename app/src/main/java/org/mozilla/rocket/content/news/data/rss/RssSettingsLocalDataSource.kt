package org.mozilla.rocket.content.news.data.rss

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
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsDataSource

class RssSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> {
        return Success(listOf(DUMMY_NEWS_LANGUAGE))
    }

    override suspend fun setSupportLanguages(languages: List<NewsLanguage>) = Unit

    override suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> {
        return Success(DUMMY_NEWS_LANGUAGE)
    }

    override suspend fun setUserPreferenceLanguage(language: NewsLanguage) = Unit

    override suspend fun getSupportCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language, "") ?: ""
            val supportCategories = ArrayList<NewsCategory>()
            toCategoryList(jsonString).let {
                supportCategories.addAll(
                    it.asSequence()
                        .mapIndexedNotNull { index, categoryId -> NewsCategory(categoryId, categoryId, getStringResourceId(categoryId), index, true) }
                        .toList()
                )
            }
            Success(supportCategories)
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

    override suspend fun getUserPreferenceCategories(language: String): Result<List<String>> {
        return Error(Exception("Not allow to customize category preferences"))
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) = Unit

    override fun shouldEnableNewsSettings() = false

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
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

        return result
    }

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX = "support_cat_"

        val DUMMY_NEWS_LANGUAGE = NewsLanguage("English", "1", "English")
    }
}
