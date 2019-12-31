package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsOnboardingRepository

class ShouldShowOnboardingUseCase(val repository: NewsOnboardingRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldShowOnboarding()
    }
}