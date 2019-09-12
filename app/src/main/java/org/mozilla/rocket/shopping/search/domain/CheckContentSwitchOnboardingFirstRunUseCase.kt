package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository
import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository.Companion.KEY_CONTENT_SWITCH_ONBOARDING

class CheckContentSwitchOnboardingFirstRunUseCase(val repository: OnboardingSharedPreferenceRepository) {
    operator fun invoke(): Boolean {
        return repository.getOnboardingPref(KEY_CONTENT_SWITCH_ONBOARDING)
    }
}