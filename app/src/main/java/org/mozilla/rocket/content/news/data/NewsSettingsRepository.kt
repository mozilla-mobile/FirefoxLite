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
    private val settingsLiveData = MediatorLiveData<Pair<NewsLanguage, List<NewsCategory>>>()
    private var remoteCategories: List<String>? = null
    private var localCategories: List<String>? = null
    private var preferenceCategories: List<String>? = null
    private var cacheNewsSettings: Pair<NewsLanguage, List<NewsCategory>>? = null

    init {
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
    }

    fun getLanguages(): LiveData<List<NewsLanguage>> {
        return languagesLiveData
    }

    fun setUserPreferenceLanguage(language: NewsLanguage) {
        localDataSource.setUserPreferenceLanguage(language)
    }

    fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {
        return localDataSource.getUserPreferenceLanguage()
    }

    fun getNewsSettings(): LiveData<Pair<NewsLanguage, List<NewsCategory>>> {
        val userPreferenceLanguageData = localDataSource.getUserPreferenceLanguage()
        settingsLiveData.removeSource(userPreferenceLanguageData)
        settingsLiveData.addSource(userPreferenceLanguageData) { newsLanguage ->
            newsLanguage?.let { language ->
                preferenceLanguage = language

                val categoriesByLanguage = getCategoriesByLanguage(language.getApiId())
                settingsLiveData.removeSource(categoriesByLanguage)
                settingsLiveData.addSource(categoriesByLanguage) { categories ->
                    categories?.let { list ->
                        val newsSettings = Pair(language, list)
                        if (newsSettings != cacheNewsSettings) {
                            cacheNewsSettings = newsSettings
                            settingsLiveData.postValue(newsSettings)
                        }
                    }
                }
            }
        }

        return settingsLiveData
    }

    fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        localDataSource.setUserPreferenceCategories(
            language,
            userPreferenceCategories.asSequence()
                .filter { it.isSelected }
                .map { it.categoryId }
                .toList()
        )
    }

    private fun updateLanguageResult() {
        val supportLanguages = ArrayList<NewsLanguage>()
        if (localLanguages?.isNotEmpty() == true) {
            localLanguages?.let {
                supportLanguages.addAll(it)
                try {
                    supportLanguages.sortBy { item -> item.code.toInt() }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }
        }

        if (remoteLanguages?.isNotEmpty() == true) {
            remoteLanguages?.let {
                localDataSource.setSupportLanguages(it)
            }
            remoteLanguages = null
        }

        val defaultLanguage = preferenceLanguage?.key ?: ""
        supportLanguages.find { it.key == defaultLanguage }?.isSelected = true

        languagesLiveData.postValue(supportLanguages)
    }

    private fun getCategoriesByLanguage(language: String): LiveData<List<NewsCategory>> {
        val remoteCategoriesData = remoteDataSource.getSupportCategories(language)
        categoriesLiveData.removeSource(remoteCategoriesData)
        categoriesLiveData.addSource(remoteCategoriesData) {
            remoteCategories = it
            updateCategoryResult(language)
        }
        val localCategoriesData = localDataSource.getSupportCategories(language)
        categoriesLiveData.removeSource(localCategoriesData)
        categoriesLiveData.addSource(localCategoriesData) {
            localCategories = it
            updateCategoryResult(language)
        }
        val localPreferenceCategoriesData = localDataSource.getUserPreferenceCategories(language)
        categoriesLiveData.removeSource(localPreferenceCategoriesData)
        categoriesLiveData.addSource(localPreferenceCategoriesData) {
            preferenceCategories = it
            updateCategoryResult(language)
        }
        return categoriesLiveData
    }

    private fun updateCategoryResult(language: String) {
        val supportCategories = ArrayList<NewsCategory>()
        if (localCategories?.isNotEmpty() == true) {
            localCategories?.let { it ->
                supportCategories.addAll(
                    it.asSequence()
                        .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                        .sortedBy { item -> item.order }
                        .toList()
                )
            }
        }
        // save all possible categories from remote to local
        if (remoteCategories?.isNotEmpty() == true) {
            remoteCategories?.let {
                // here's a infinite loop here, cause localDataSource will make it's obeserver call updateCategoryResult
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
