package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice

class SetSetDefaultBrowserOnboardingIsShownUseCase(private val newFeatureNotice: NewFeatureNotice) {
    operator fun invoke() {
        newFeatureNotice.setSetDefaultBrowserOnboardingDidShow()
    }
}