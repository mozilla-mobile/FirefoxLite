package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.SharedPreferences
import org.mozilla.threadutils.ThreadUtils

class NewsSettingsLocalDataSource(private val context: Context) : NewsSettingsDataSource {
    private val languagesLiveData: MutableLiveData<List<NewsLanguage>> = MutableLiveData()
    private val preferenceLanguagesLiveData: MutableLiveData<NewsLanguage> = MutableLiveData()

    companion object {
        private const val PREF_NAME = "news_settings"
        private const val KEY_STRING_SUPPORT_LANGUAGES = "support_lang"
        private const val KEY_STRING_USER_PREFERENCE_LANGUAGE = "user_pref_lang"
    }

    override fun getSupportLanguages(): LiveData<List<NewsLanguage>> {
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences()
                .getString(KEY_STRING_SUPPORT_LANGUAGES, "") ?: ""
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
        ThreadUtils.postToBackgroundThread {
            val jsonString = getPreferences()
                .getString(KEY_STRING_USER_PREFERENCE_LANGUAGE, "") ?: ""
            val newsLanguageList = NewsLanguage.fromJson(jsonString)
            if (newsLanguageList.isNotEmpty()) {
                preferenceLanguagesLiveData.postValue(newsLanguageList[0].also { it.isSelected = true })
            }
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserPreferenceCategories(language: String): LiveData<List<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}