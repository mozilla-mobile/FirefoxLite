package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.combineLatest
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository

class HasUnreadMissionsUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): LiveData<Boolean> {
        val missions = missionRepository.getMissions()
        val readMissionIds: LiveData<List<String>> = missionRepository.getReadMissionIdsLiveData()
        return combineLatest(missions, readMissionIds)
                .map { (missions, readIds) ->
                    hasUnreadMissions(missions, readIds)
                }
    }

    private fun hasUnreadMissions(missions: List<Mission>, readIds: List<String>): Boolean {
        missions.forEach {
            val isUnread = isUnreadMission(it, readIds)
            if (isUnread) {
                return true
            }
        }
        return false
    }

    private fun isUnreadMission(mission: Mission, readIds: List<String>): Boolean =
            mission.status == Mission.STATUS_NEW && !readIds.contains(mission.uniqueId)
}