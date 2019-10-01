package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider

class SetUserPreferenceCategoriesUseCase(repositoryProvider: NewsSettingsRepositoryProvider) {

    val repository = repositoryProvider.provideNewsSettingsRepository()

    suspend operator fun invoke(
        language: String,
        userPreferenceCategories: List<NewsCategory>
    ) {
        repository.setUserPreferenceCategories(language, userPreferenceCategories)
    }
}
