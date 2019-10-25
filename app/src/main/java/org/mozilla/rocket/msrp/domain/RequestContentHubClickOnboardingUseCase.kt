package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class RequestContentHubClickOnboardingUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(couponName: String) {
        missionRepository.requestContentHubClickOnboarding(couponName)
    }
}