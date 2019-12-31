package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsOnboardingRepository

class SetOnboardingHasShownUseCase(val repository: NewsOnboardingRepository) {
    operator fun invoke() {
        repository.setOnboardingHasShown()
    }
}