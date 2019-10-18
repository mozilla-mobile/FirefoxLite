package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelOnboardingRepository

class ShouldShowOnboardingUseCase(val repository: TravelOnboardingRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldShowOnboarding()
    }
}