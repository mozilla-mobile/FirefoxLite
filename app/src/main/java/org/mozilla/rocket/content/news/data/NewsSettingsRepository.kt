package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.succeeded

class NewsSettingsRepository(
    private val remoteDataSource: NewsSettingsDataSource,
    private val localDataSource: NewsSettingsDataSource
) {

    suspend fun getLanguages(): Result<List<NewsLanguage>> {
        val remoteResult = remoteDataSource.getSupportLanguages()
        if (remoteResult is Result.Success && remoteResult.data.isNotEmpty()) {
            localDataSource.setSupportLanguages(remoteResult.data)
        }

        val supportLanguagesResult = localDataSource.getSupportLanguages()
        if (supportLanguagesResult is Result.Success && supportLanguagesResult.data.isNotEmpty()) {
            val supportLanguages = supportLanguagesResult.data
            val defaultLanguageResult = localDataSource.getUserPreferenceLanguage()
            if (defaultLanguageResult is Result.Success && defaultLanguageResult.succeeded) {
                supportLanguages.forEach {
                    it.isSelected = (it.key == defaultLanguageResult.data!!.key) // succeeded implies data is not null
                }
            }
            return Result.Success(supportLanguages)
        }

        return Result.Error(Exception("Fail to get news languages result"))
    }

    suspend fun setUserPreferenceLanguage(language: NewsLanguage) {
        localDataSource.setUserPreferenceLanguage(language)
    }

    suspend fun getNewsSettings(defaultLanguage: NewsLanguage): Result<Pair<NewsLanguage, List<NewsCategory>>> {
        val userPreferenceLanguageResult = localDataSource.getUserPreferenceLanguage()
        val userPreferenceLanguage =
            if (userPreferenceLanguageResult is Result.Success && userPreferenceLanguageResult.data != null) {
                userPreferenceLanguageResult.data
            } else {
                defaultLanguage
            }

        val categoriesByLanguageResult = getCategoriesByLanguage(userPreferenceLanguage.apiId)
        if (categoriesByLanguageResult is Result.Success && categoriesByLanguageResult.data.isNotEmpty()) {
            val supportCategories = categoriesByLanguageResult.data
            return Result.Success(Pair(userPreferenceLanguage, supportCategories))
        }

        return Result.Error(Exception("Fail to get news settings result"))
    }

    suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        localDataSource.setUserPreferenceCategories(
            language,
            userPreferenceCategories.asSequence()
                .filter { it.isSelected }
                .map { it.categoryId }
                .toList()
        )
    }

    private suspend fun getCategoriesByLanguage(language: String): Result<List<NewsCategory>> {
        val remoteCategoriesResult = remoteDataSource.getSupportCategories(language)
        if (remoteCategoriesResult is Result.Success && remoteCategoriesResult.isNotEmpty) {
            localDataSource.setSupportCategories(language, remoteCategoriesResult.data)
        }

        val supportCategories = ArrayList<NewsCategory>()
        val localCategoriesResult = localDataSource.getSupportCategories(language)
        if (localCategoriesResult is Result.Success && localCategoriesResult.isNotEmpty) {
            localCategoriesResult.data.let {
                supportCategories.addAll(
                    it.asSequence()
                        .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                        .sortedBy { item -> item.order }
                        .toList()
                )
            }
        }

        val preferenceCategoriesResult = localDataSource.getUserPreferenceCategories(language)
        if (preferenceCategoriesResult is Result.Success && preferenceCategoriesResult.isNotEmpty) {
            val selectedCategories = preferenceCategoriesResult.data.joinToString(",")
            if (selectedCategories.isNotEmpty()) {
                supportCategories.forEach {
                    it.isSelected = selectedCategories.contains(it.categoryId)
                }
            }
        }

        return Result.Success(supportCategories)
    }
}
