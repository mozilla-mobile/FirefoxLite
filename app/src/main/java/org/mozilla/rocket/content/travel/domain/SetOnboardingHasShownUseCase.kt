package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelOnboardingRepository

class SetOnboardingHasShownUseCase(val repository: TravelOnboardingRepository) {
    operator fun invoke() {
        repository.setOnboardingHasShown()
    }
}