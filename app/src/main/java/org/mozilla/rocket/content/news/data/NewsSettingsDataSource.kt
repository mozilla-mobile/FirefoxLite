package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData

interface NewsSettingsDataSource {
    fun getSupportLanguages(): LiveData<List<NewsLanguage>>
    fun setSupportLanguages(languages: List<NewsLanguage>)
    fun getUserPreferenceLanguage(): LiveData<NewsLanguage>
    fun setUserPreferenceLanguage(language: NewsLanguage)
    fun getSupportCategories(language: String): LiveData<List<String>>
    fun getUserPreferenceCategories(language: String): LiveData<List<String>>
}