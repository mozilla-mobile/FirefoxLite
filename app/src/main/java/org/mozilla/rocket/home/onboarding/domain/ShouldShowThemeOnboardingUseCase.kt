package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice

class ShouldShowThemeOnboardingUseCase(private val newFeatureNotice: NewFeatureNotice) {
    operator fun invoke(): Boolean = !newFeatureNotice.hasHomeThemeOnboardingShown()
}