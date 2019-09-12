package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository
import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository.Companion.KEY_ONBOARDING

class CompleteOnboardingFirstRunUseCase(val repository: OnboardingSharedPreferenceRepository) {
    operator fun invoke() {
        repository.setOnboardingPref(KEY_ONBOARDING, false)
    }
}