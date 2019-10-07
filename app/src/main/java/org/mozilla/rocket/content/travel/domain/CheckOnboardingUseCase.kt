package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelOnboardingRepository

class CheckOnboardingUseCase(val repository: TravelOnboardingRepository) {
    operator fun invoke(): Boolean {
        return repository.getOnboardingPref(TravelOnboardingRepository.KEY_ONBOARDING)
    }
}