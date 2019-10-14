package org.mozilla.rocket.home.onboarding

import org.mozilla.focus.utils.NewFeatureNotice

class CompleteHomeOnboardingUseCase(private val newFeatureNotice: NewFeatureNotice) {
    operator fun invoke() {
        newFeatureNotice.setHomePageOnboardingDidShow()
    }
}