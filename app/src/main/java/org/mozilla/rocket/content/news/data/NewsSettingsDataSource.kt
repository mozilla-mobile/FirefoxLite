package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.Result

interface NewsSettingsDataSource {
    suspend fun getSupportLanguages(): Result<List<NewsLanguage>>
    suspend fun setSupportLanguages(languages: List<NewsLanguage>)
    suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?>
    suspend fun setUserPreferenceLanguage(language: NewsLanguage)
    suspend fun getSupportCategories(language: String): Result<List<NewsCategory>>
    suspend fun setSupportCategories(language: String, supportCategories: List<NewsCategory>)
    suspend fun getUserPreferenceCategories(language: String): Result<List<NewsCategory>>
    suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>)
    fun getDefaultLanguage(): NewsLanguage
    fun getDefaultCategory(): NewsCategory
    fun getAdditionalSourceInfo(): NewsSourceInfo?
    fun shouldEnableNewsSettings(): Boolean
    fun shouldEnablePersonalizedNews(): Boolean
    fun hasUserEnabledPersonalizedNews(): Boolean
    fun setUserEnabledPersonalizedNews(enable: Boolean)
    fun shouldShowPersonalizedNewsOnboarding(): Boolean
    fun setPersonalizedNewsOnboardingHasShown()
    fun shouldShowNewsLanguageSettingPage(): Boolean
    fun setNewsLanguageSettingPageState(enable: Boolean)
}