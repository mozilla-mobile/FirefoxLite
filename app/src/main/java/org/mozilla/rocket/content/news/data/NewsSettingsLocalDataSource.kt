package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.threadutils.ThreadUtils

class NewsSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {
    private val languagesLiveData: MutableLiveData<List<NewsLanguage>> = MutableLiveData()
    private val preferenceLanguagesLiveData: MutableLiveData<NewsLanguage> = MutableLiveData()
    private val supportCategoriesLiveData: MutableLiveData<List<String>> = MutableLiveData()
    private val preferenceCategoriesLiveData: MutableLiveData<List<String>> = MutableLiveData()

    companion object {
        private const val TAG = "NewsLocalDataSource"
        private const val PREF_NAME = "news_settings"
        private const val KEY_STRING_SUPPORT_LANGUAGES = "support_lang"
        private const val KEY_STRING_USER_PREFERENCE_LANGUAGE = "user_pref_lang"
        private const val KEY_STRING_SUPPORT_CATEGORIES_PREFIX = "support_cat_"
        private const val KEY_STRING_USER_PREFERENCE_CATEGORIES_PREFIX = "user_pref_cat_"
        private const val DEFAULT_LANGUAGE = "english"
        private const val DEFAULT_LANGUAGE_KEY = "1"
    }

    override fun getSupportLanguages(): LiveData<List<NewsLanguage>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences()
                .getString(KEY_STRING_SUPPORT_LANGUAGES, DEFAULT_LANGUAGE) ?: ""
            val newsLanguageList = NewsLanguage.fromJson(jsonString)
            if (newsLanguageList.isNotEmpty()) {
                languagesLiveData.postValue(newsLanguageList)
            }
        }

        return languagesLiveData
    }

    override fun setSupportLanguages(languages: List<NewsLanguage>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(KEY_STRING_SUPPORT_LANGUAGES, languages.toJson().toString()).apply()
        }
        languagesLiveData.postValue(languages)
    }

    override fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {

        try {

            val jsonString = getPreferences()
                .getString(KEY_STRING_USER_PREFERENCE_LANGUAGE, "") ?: ""

            val selectedLanguage = if (TextUtils.isEmpty(jsonString)) {
                NewsLanguage(DEFAULT_LANGUAGE_KEY, DEFAULT_LANGUAGE, DEFAULT_LANGUAGE, true)
            } else {
                val newsLanguageList = NewsLanguage.fromJson(jsonString)
                if (newsLanguageList.isNotEmpty()) {
                    newsLanguageList[0].also { it.isSelected = true }
                }
                null
            }
            preferenceLanguagesLiveData.postValue(selectedLanguage)
        } catch (e: JSONException) {
            Log.d(TAG, "Error Parsing Json")
        }

        return preferenceLanguagesLiveData
    }

    override fun setUserPreferenceLanguage(language: NewsLanguage) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(KEY_STRING_USER_PREFERENCE_LANGUAGE, language.toJson().toString()).apply()
        }
        preferenceLanguagesLiveData.postValue(language)
    }

    override fun getSupportCategories(language: String): LiveData<List<String>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences().getString(KEY_STRING_SUPPORT_CATEGORIES_PREFIX + language, "")
            jsonString?.let {
                supportCategoriesLiveData.postValue(toCategoryList(it))
            }
        }

        return supportCategoriesLiveData
    }

    override fun setSupportCategories(language: String, supportCategories: List<String>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(
                KEY_STRING_SUPPORT_CATEGORIES_PREFIX + language,
                categoryListToJsonArray(supportCategories).toString()
            ).apply()
        }
        supportCategoriesLiveData.postValue(supportCategories)
    }

    override fun getUserPreferenceCategories(language: String): LiveData<List<String>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences().getString(KEY_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language, "")
            jsonString?.let {
                preferenceCategoriesLiveData.postValue(toCategoryList(it))
            }
        }

        return preferenceCategoriesLiveData
    }

    override fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) {
        ThreadUtils.postToBackgroundThread {
            getPreferences().edit().putString(
                KEY_STRING_USER_PREFERENCE_CATEGORIES_PREFIX + language,
                categoryListToJsonArray(userPreferenceCategories).toString()
            ).apply()
        }
        preferenceCategoriesLiveData.postValue(userPreferenceCategories)
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
        val items = JSONArray(jsonString)
        for (i in 0 until items.length()) {
            val categoryId = items.optString(i)
            result.add(categoryId)
        }

        return result
    }
}