package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class ReadMissionUseCase(
    private val missionRepository: MissionRepository
) {

    suspend operator fun invoke(missionId: String) {
        missionRepository.addReadMissionId(missionId)
    }
}