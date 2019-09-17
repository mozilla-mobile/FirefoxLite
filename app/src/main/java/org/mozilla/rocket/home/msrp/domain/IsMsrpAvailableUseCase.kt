package org.mozilla.rocket.home.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class IsMsrpAvailableUseCase(private val missionRepo: MissionRepository) {

    operator fun invoke(): Boolean {
        return missionRepo.isMsrpAvailable()
    }
}
