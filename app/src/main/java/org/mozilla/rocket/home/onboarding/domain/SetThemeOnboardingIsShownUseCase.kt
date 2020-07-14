package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice

class SetThemeOnboardingIsShownUseCase(private val newFeatureNotice: NewFeatureNotice) {
    operator fun invoke() {
        newFeatureNotice.setHomeThemeOnboardingDidShow()
    }
}