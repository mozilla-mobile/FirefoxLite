package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class SetUserPreferenceCategoriesUseCase(private val repository: NewsSettingsRepository) {

    suspend operator fun invoke(
        language: String,
        userPreferenceCategories: List<NewsCategory>
    ) {
        repository.setUserPreferenceCategories(language, userPreferenceCategories)
    }
}
