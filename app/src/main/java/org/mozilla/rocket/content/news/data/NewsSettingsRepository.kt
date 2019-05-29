package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations

class NewsSettingsRepository(
    private val remoteDataSource: NewsSettingsDataSource,
    private val localDataSource: NewsSettingsDataSource
) {

    private val languagesLiveData = MediatorLiveData<List<NewsLanguage>>()
    private var remoteLanguages: List<NewsLanguage>? = null
    private var localLanguages: List<NewsLanguage>? = null
    private var preferenceLanguage: NewsLanguage? = null

    fun getLanguages(): LiveData<List<NewsLanguage>> {
        languagesLiveData.addSource(remoteDataSource.getSupportLanguages()) {
            remoteLanguages = it
            updateLanguageResult()
        }
        languagesLiveData.addSource(localDataSource.getSupportLanguages()) {
            localLanguages = it
            updateLanguageResult()
        }
        languagesLiveData.addSource(localDataSource.getUserPreferenceLanguage()) {
            preferenceLanguage = it
            updateLanguageResult()
        }
        return languagesLiveData
    }

    fun setUserPreferenceLanguage(language: NewsLanguage) {
        localDataSource.setUserPreferenceLanguage(language)
    }

    fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {
        return localDataSource.getUserPreferenceLanguage()
    }

    fun getCategoriesByLanguage(language: String): LiveData<List<NewsCategory>> {
        return Transformations.map(remoteDataSource.getSupportCategories(language)) {
            it.asSequence()
                .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                .toList()
        }
    }

    private fun updateLanguageResult() {
        val supportLanguages = ArrayList<NewsLanguage>()
        if (localLanguages?.isNotEmpty() == true) {
            localLanguages?.let {
                supportLanguages.addAll(it)
            }
        }

        if (remoteLanguages?.isNotEmpty() == true) {
            remoteLanguages?.let {
                localDataSource.setSupportLanguages(it)
            }
            remoteLanguages = null
        }

        val defaultLanguage = preferenceLanguage?.key ?: "English"
        supportLanguages.find { it.key == defaultLanguage }?.isSelected = true

        languagesLiveData.postValue(supportLanguages)
    }
}