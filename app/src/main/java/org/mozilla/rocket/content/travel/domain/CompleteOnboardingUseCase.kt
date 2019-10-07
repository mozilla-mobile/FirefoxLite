package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelOnboardingRepository

class CompleteOnboardingUseCase(val repository: TravelOnboardingRepository) {
    operator fun invoke() {
        repository.setOnboardingPref(TravelOnboardingRepository.KEY_ONBOARDING, false)
    }
}