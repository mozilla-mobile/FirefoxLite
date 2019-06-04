package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.threadutils.ThreadUtils

class NewsSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {
    private val languagesLiveData: MutableLiveData<List<NewsLanguage>> = MutableLiveData()
    private val preferenceLanguagesLiveData: MutableLiveData<NewsLanguage> = MutableLiveData()
    private val supportCategoriesLiveData: MutableLiveData<List<String>> = MutableLiveData()
    private val preferenceCategoriesLiveData: MutableLiveData<List<String>> = MutableLiveData()

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_JSON_STRING_SUPPORT_LANGUAGES = "support_lang"
        private const val KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE = "user_pref_lang"
        private const val KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX = "support_cat_"
        private const val KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX = "user_pref_cat_"
        private val DEFAULT_LANGUAGE_LIST = listOf(
            NewsLanguage("English", "1", "English")
        )
        private val DEFAULT_CATEGORY_LIST = listOf(
            NewsCategory.TOP_NEWS.categoryId
        )
    }

    override fun getSupportLanguages(): LiveData<List<NewsLanguage>> {
        ThreadUtils.postToBackgroundThread {
            var newsLanguageList = DEFAULT_LANGUAGE_LIST
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_SUPPORT_LANGUAGES, "") ?: ""
            if (!TextUtils.isEmpty(jsonString)) {
                try {
                    val newsLanguageResult = NewsLanguage.fromJson(jsonString)
                    if (newsLanguageResult.isNotEmpty()) {
                        newsLanguageList = newsLanguageResult
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            languagesLiveData.postValue(newsLanguageList)
        }

        return languagesLiveData
    }

    override fun setSupportLanguages(languages: List<NewsLanguage>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(KEY_JSON_STRING_SUPPORT_LANGUAGES, languages.toJson().toString()).apply()
        }
        languagesLiveData.postValue(languages)
    }

    override fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {
        ThreadUtils.postToBackgroundThread {
            var selectedLanguage = DEFAULT_LANGUAGE_LIST[0].also { it.isSelected = true }
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
            preferenceLanguagesLiveData.postValue(selectedLanguage)
        }

        return preferenceLanguagesLiveData
    }

    override fun setUserPreferenceLanguage(language: NewsLanguage) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(KEY_JSON_STRING_USER_PREFERENCE_LANGUAGE, language.toJson().toString()).apply()
        }
        preferenceLanguagesLiveData.postValue(language)
    }

    override fun getSupportCategories(language: String): LiveData<List<String>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language, "") ?: ""
            supportCategoriesLiveData.postValue(toCategoryList(jsonString))
        }

        return supportCategoriesLiveData
    }

    override fun setSupportCategories(language: String, supportCategories: List<String>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(
                KEY_JSON_STRING_SUPPORT_CATEGORIES_PREFIX + language,
                categoryListToJsonArray(supportCategories).toString()
            ).apply()
        }
        supportCategoriesLiveData.postValue(supportCategories)
    }

    override fun getUserPreferenceCategories(language: String): LiveData<List<String>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences()
                .getString(KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language, "") ?: ""
            if (jsonString.isNotEmpty()) {
                preferenceCategoriesLiveData.postValue(toCategoryList(jsonString))
            }
        }

        return preferenceCategoriesLiveData
    }

    override fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(
                KEY_JSON_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language,
                categoryListToJsonArray(userPreferenceCategories).toString()
            ).apply()
        }
//        preferenceCategoriesLiveData.postValue(userPreferenceCategories)
    }

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

        return if (result.size > 0) {
            result
        } else {
            DEFAULT_CATEGORY_LIST
        }
    }
}