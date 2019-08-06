package org.mozilla.rocket.content.news.data

import kotlinx.coroutines.runBlocking
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty

class NewsSettingsRepository(
    private val remoteDataSource: NewsSettingsDataSource,
    private val localDataSource: NewsSettingsDataSource
) {

//    private val languagesLiveData = MediatorLiveData<List<NewsLanguage>>()
//    private var remoteLanguages: List<NewsLanguage>? = null
//    private var localLanguages: List<NewsLanguage>? = null
//    private var preferenceLanguage: NewsLanguage? = null

//    private val categoriesLiveData = MediatorLiveData<List<NewsCategory>>()
//    private val settingsLiveData = MediatorLiveData<Pair<NewsLanguage, List<NewsCategory>>>()
//    private var remoteCategories: List<String>? = null
//    private var localCategories: List<String>? = null
//    private var preferenceCategories: List<String>? = null
//    private var cacheNewsSettings: Pair<NewsLanguage, List<NewsCategory>>? = null

//    init {
//        languagesLiveData.addSource(remoteDataSource.getSupportLanguages()) {
//            remoteLanguages = it
//            updateLanguageResult()
//        }
//        languagesLiveData.addSource(localDataSource.getSupportLanguages()) {
//            localLanguages = it
//            updateLanguageResult()
//        }
//        languagesLiveData.addSource(localDataSource.getUserPreferenceLanguage()) {
//            preferenceLanguage = it
//            updateLanguageResult()
//        }
//    }

    suspend fun getLanguages(): Result<List<NewsLanguage>> {
        val localResult = localDataSource.getSupportLanguages()
        if (localResult.isNotEmpty) {
            return localResult
        }

        val remoteResult = remoteDataSource.getSupportLanguages()
        if (remoteResult is Result.Success && remoteResult.data.isNotEmpty()) {
            localDataSource.setSupportLanguages(remoteResult.data)
        }

        return remoteResult
    }

    fun setUserPreferenceLanguage(language: NewsLanguage) {
        runBlocking {
            localDataSource.setUserPreferenceLanguage(language)
        }
    }

    suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> {
        return localDataSource.getUserPreferenceLanguage()
    }

    suspend fun getNewsSettings(): Result<Pair<NewsLanguage, List<NewsCategory>>> {
        val userPreferenceLanguageResult = localDataSource.getUserPreferenceLanguage()
        if (userPreferenceLanguageResult is Result.Success && userPreferenceLanguageResult.data != null) {
            val userPreferenceLanguage = userPreferenceLanguageResult.data
            val categoriesByLanguageResult = remoteDataSource.getSupportCategories(userPreferenceLanguage.getApiId())
            if (categoriesByLanguageResult is Result.Success && categoriesByLanguageResult.data.isNotEmpty()) {
                val supportCategories = ArrayList<NewsCategory>()
                categoriesByLanguageResult.data.let {
                    supportCategories.addAll(
                        it.asSequence()
                            .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                            .sortedBy { item -> item.order }
                            .toList()
                    )
                }
                return Result.Success(Pair(userPreferenceLanguage, supportCategories))
            }
        }

        return Result.Error(Exception("Fail to get news settings result"))
    }

    fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        runBlocking {
            localDataSource.setUserPreferenceCategories(
                language,
                userPreferenceCategories.asSequence()
                    .filter { it.isSelected }
                    .map { it.categoryId }
                    .toList()
            )
        }
    }

//    private fun updateLanguageResult() {
//        val supportLanguages = ArrayList<NewsLanguage>()
//        if (localLanguages?.isNotEmpty() == true) {
//            localLanguages?.let {
//                val displayCharacterForNotSupportedCharacter = "\u2612"
//                val characterValidator = CharacterValidator(displayCharacterForNotSupportedCharacter)
//                supportLanguages.addAll(it.filterNot { item -> characterValidator.characterIsMissingInFont(item.name.substring(0, 1)) })
//                try {
//                    supportLanguages.sortBy { item -> item.code.toInt() }
//                } catch (e: NumberFormatException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//
//        if (remoteLanguages?.isNotEmpty() == true) {
//            remoteLanguages?.let {
//                localDataSource.setSupportLanguages(it)
//            }
//            remoteLanguages = null
//        }
//
//        val defaultLanguage = preferenceLanguage?.key ?: ""
//        supportLanguages.forEach {
//            it.isSelected = (it.key == defaultLanguage)
//        }
//
//        languagesLiveData.postValue(supportLanguages)
//    }

//    private fun getCategoriesByLanguage(language: String): LiveData<List<NewsCategory>> {
//        val remoteCategoriesData = remoteDataSource.getSupportCategories(language)
//        categoriesLiveData.removeSource(remoteCategoriesData)
//        categoriesLiveData.addSource(remoteCategoriesData) {
//            remoteCategories = it
//            updateCategoryResult(language)
//        }
//        val localCategoriesData = localDataSource.getSupportCategories(language)
//        categoriesLiveData.removeSource(localCategoriesData)
//        categoriesLiveData.addSource(localCategoriesData) {
//            localCategories = it
//            updateCategoryResult(language)
//        }
//        val localPreferenceCategoriesData = localDataSource.getUserPreferenceCategories(language)
//        categoriesLiveData.removeSource(localPreferenceCategoriesData)
//        categoriesLiveData.addSource(localPreferenceCategoriesData) {
//            preferenceCategories = it
//            updateCategoryResult(language)
//        }
//        return categoriesLiveData
//    }

//    private fun updateCategoryResult(language: String) {
//        val supportCategories = ArrayList<NewsCategory>()
//        if (localCategories?.isNotEmpty() == true) {
//            localCategories?.let { it ->
//                supportCategories.addAll(
//                    it.asSequence()
//                        .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
//                        .sortedBy { item -> item.order }
//                        .toList()
//                )
//            }
//        }
//        // save all possible categories from remote to local
//        if (remoteCategories?.isNotEmpty() == true) {
//            remoteCategories?.let {
//                // here's a infinite loop here, cause localDataSource will make it's obeserver call updateCategoryResult
//                localDataSource.setSupportCategories(language, it)
//            }
//            remoteCategories = null
//        }
//
//        val selectedCategories = preferenceCategories?.joinToString(",") ?: ""
//        if (selectedCategories.isNotEmpty()) {
//            supportCategories.forEach {
//                it.isSelected = selectedCategories.contains(it.categoryId)
//            }
//        }
//
//        categoriesLiveData.postValue(supportCategories)
//    }
}
