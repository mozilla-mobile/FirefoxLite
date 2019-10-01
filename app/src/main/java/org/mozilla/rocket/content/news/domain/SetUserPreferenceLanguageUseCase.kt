package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider

class SetUserPreferenceLanguageUseCase(repositoryProvider: NewsSettingsRepositoryProvider) {

    val repository = repositoryProvider.provideNewsSettingsRepository()

    suspend operator fun invoke(language: NewsLanguage) {
        repository.setUserPreferenceLanguage(language)
    }
}
