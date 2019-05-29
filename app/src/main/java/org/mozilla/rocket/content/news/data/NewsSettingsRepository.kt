package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData

class NewsSettingsRepository(
    private val remoteDataSource: NewsSettingsDataSource,
    private val localDataSource: NewsSettingsDataSource
) {

    private val languagesLiveData = MediatorLiveData<List<NewsLanguage>>()
    private var remoteLanguages: List<NewsLanguage>? = null
    private var localLanguages: List<NewsLanguage>? = null
    private var preferenceLanguage: NewsLanguage? = null

    private val categoriesLiveData = MediatorLiveData<List<NewsCategory>>()
    private var remoteCategories: List<String>? = null
    private var localCategories: List<String>? = null
    private var preferenceCategories: List<String>? = null

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
        categoriesLiveData.addSource(remoteDataSource.getSupportCategories(language)) {
            remoteCategories = it
            updateCategoryResult(language)
        }
        categoriesLiveData.addSource(localDataSource.getSupportCategories(language)) {
            localCategories = it
            updateCategoryResult(language)
        }
        categoriesLiveData.addSource(localDataSource.getUserPreferenceCategories(language)) {
            preferenceCategories = it
            updateCategoryResult(language)
        }
        return categoriesLiveData
    }

    fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        localDataSource.setUserPreferenceCategories(
            language,
            userPreferenceCategories.asSequence().map { it.categoryId }.toList()
        )
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

    private fun updateCategoryResult(language: String) {
        val supportCategories = ArrayList<NewsCategory>()
        if (localCategories?.isNotEmpty() == true) {
            localCategories?.let {
                supportCategories.addAll(
                    it.asSequence()
                        .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                        .toList()
                )
            }
        }

        if (remoteCategories?.isNotEmpty() == true) {
            remoteCategories?.let {
                localDataSource.setSupportCategories(language, it)
            }
            remoteCategories = null
        }

        val selectedCategories = preferenceCategories?.joinToString(",") ?: ""
        if (selectedCategories.isNotEmpty()) {
            supportCategories.forEach {
                it.isSelected = selectedCategories.contains(it.categoryId)
            }
        }

        categoriesLiveData.postValue(supportCategories)
    }
}