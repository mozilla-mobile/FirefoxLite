package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository

class GetRedeemMissionsUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): LiveData<List<Mission>> {
        return missionRepository.getMissions()
                .map { missions ->
                    val (_, redeemList) = divideMissions(missions)
                    redeemList
                }
    }

    private fun divideMissions(missions: List<Mission>): Pair<List<Mission>, List<Mission>> {
        val map = missions.groupBy {
            it.status == Mission.STATUS_NEW || it.status == Mission.STATUS_JOINED
        }
        val challengeList = map[true] ?: emptyList()
        val redeemList = map[false] ?: emptyList()
        return challengeList to redeemList
    }
}