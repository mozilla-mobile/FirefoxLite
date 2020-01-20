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

    suspend fun getNewsSettings(defaultLanguage: NewsLanguage): Result<NewsSettings> {
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
            val shouldEnableNewsSettings = localDataSource.shouldEnableNewsSettings()
            return Result.Success(NewsSettings(userPreferenceLanguage, supportCategories, shouldEnableNewsSettings))
        }

        return Result.Error(Exception("Fail to get news settings result"))
    }

    suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        localDataSource.setUserPreferenceCategories(
            language,
            userPreferenceCategories.filter { it.isSelected }
        )
    }

    fun getDefaultLanguage() = localDataSource.getDefaultLanguage()

    fun getDefaultCategory() = localDataSource.getDefaultCategory()

    fun getAdditionalSourceInfo(): NewsSourceInfo? = localDataSource.getAdditionalSourceInfo()

    fun shouldEnablePersonalizedNews() = remoteDataSource.shouldEnablePersonalizedNews()

    fun hasUserEnabledPersonalizedNews() = localDataSource.hasUserEnabledPersonalizedNews()

    fun setUserEnabledPersonalizedNews(enable: Boolean) = localDataSource.setUserEnabledPersonalizedNews(enable)

    fun shouldShowPersonalizedNewsOnboarding() = localDataSource.shouldShowPersonalizedNewsOnboarding()

    fun setPersonalizedNewsOnboardingHasShown() = localDataSource.setPersonalizedNewsOnboardingHasShown()

    fun shouldShowNewsLanguageSettingPage() = localDataSource.shouldShowNewsLanguageSettingPage()

    fun setNewsLanguageSettingPageState(enable: Boolean) = localDataSource.setNewsLanguageSettingPageState(enable)

    private suspend fun getCategoriesByLanguage(language: String): Result<List<NewsCategory>> {
        val remoteCategoriesResult = remoteDataSource.getSupportCategories(language)
        if (remoteCategoriesResult is Result.Success && remoteCategoriesResult.isNotEmpty) {
            localDataSource.setSupportCategories(language, remoteCategoriesResult.data)
        }

        val supportCategories = ArrayList<NewsCategory>()
        val localCategoriesResult = localDataSource.getSupportCategories(language)
        if (localCategoriesResult is Result.Success && localCategoriesResult.isNotEmpty) {
            localCategoriesResult.data.let {
                supportCategories.addAll(it)
            }
        }

        val preferenceCategoriesResult = localDataSource.getUserPreferenceCategories(language)
        if (preferenceCategoriesResult is Result.Success && preferenceCategoriesResult.isNotEmpty) {
            val selectedCategories = preferenceCategoriesResult.data.joinToString(",") { it.name }
            if (selectedCategories.isNotEmpty()) {
                supportCategories.forEach {
                    it.isSelected = selectedCategories.contains(it.name)
                }
            }
        }

        return Result.Success(supportCategories)
    }
}
