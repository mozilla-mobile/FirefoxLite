package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository
import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository.Companion.KEY_CONTENT_SWITCH_ONBOARDING

class CompleteContentSwitchOnboardingFirstRunUseCase(val repository: OnboardingSharedPreferenceRepository) {
    operator fun invoke() {
        repository.setOnboardingPref(KEY_CONTENT_SWITCH_ONBOARDING, false)
    }
}