package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class IsNeedJoinMissionOnboardingUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): Boolean = !missionRepository.hasJoinedAnyMissionBefore()
}