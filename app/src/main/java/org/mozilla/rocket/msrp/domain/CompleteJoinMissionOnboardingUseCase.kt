package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class CompleteJoinMissionOnboardingUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke() {
        missionRepository.setJoinedAnyMissionBefore()
    }
}
