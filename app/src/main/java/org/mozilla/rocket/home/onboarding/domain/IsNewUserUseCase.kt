package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice

class IsNewUserUseCase(private val newFeatureNotice: NewFeatureNotice) {

    operator fun invoke(): Boolean = newFeatureNotice.lastShownFeatureVersion == 0
}