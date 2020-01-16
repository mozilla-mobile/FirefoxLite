package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class SetPersonalizedNewsOnboardingHasShownUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke() {
        repository.setPersonalizedNewsOnboardingHasShown()
    }
}