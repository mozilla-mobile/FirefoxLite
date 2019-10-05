package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.combineLatest
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository

class GetChallengeMissionsUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): LiveData<List<Mission>> {
        val missions = missionRepository.getMissions()
        val readMissionIds: LiveData<List<String>> = missionRepository.getReadMissionIdsLiveData()
        return combineLatest(missions, readMissionIds)
                .map { (missions, readIds) ->
                    val (challengeList, _) = divideMissions(missions)
                    challengeList.apply {
                        initUnread(readIds)
                    }
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

    private fun List<Mission>.initUnread(readIds: List<String>) {
        forEach { it.unread = isUnreadMission(it, readIds) }
    }

    private fun isUnreadMission(mission: Mission, readIds: List<String>): Boolean =
            mission.status == Mission.STATUS_NEW && !readIds.contains(mission.uniqueId)
}