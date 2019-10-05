package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class IsMsrpAvailableUseCase(private val missionRepo: MissionRepository) {

    operator fun invoke(): Boolean {
        return missionRepo.isMsrpAvailable()
    }
}
