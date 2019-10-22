package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice

class ShouldShowShoppingSearchOnboardingUseCase(private val newFeatureNotice: NewFeatureNotice) {
    operator fun invoke(): Boolean {
        return !newFeatureNotice.hasHomeShoppingSearchOnboardingShown()
    }
}