package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class SetUserPreferenceLanguageUseCase(private val repository: NewsSettingsRepository) {

    suspend operator fun invoke(language: NewsLanguage) {
        repository.setUserPreferenceLanguage(language)
    }
}
