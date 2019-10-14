package org.mozilla.rocket.home.onboarding

import org.mozilla.focus.utils.NewFeatureNotice

class IsNeedToShowHomeOnboardingUseCase(private val newFeatureNotice: NewFeatureNotice) {

    operator fun invoke(): Boolean = !newFeatureNotice.hasHomePageOnboardingShown()
}