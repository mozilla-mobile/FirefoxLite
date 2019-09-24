package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.mozilla.rocket.extension.combineLatest
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.getNotNull

class LoadMissionsUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    private val missionsLiveData = MutableLiveData<Result<List<Mission>, RewardServiceError>>()
    private val readMissionIdsLiveData: LiveData<List<String>> = missionRepository.getReadMissionIdsLiveData()
    private val resultLiveData: LiveData<Result<Pair<List<Mission>, List<Mission>>, Error>>

    init {
        resultLiveData = combineLatest(missionsLiveData, readMissionIdsLiveData)
                .map { (missionsResult, readIds) ->
                    val missions = missionsResult.getNotNull { error ->
                        return@map Result.error<Pair<List<Mission>, List<Mission>>, Error>(error = parseRewardError(error))
                    }
                    val (challengeList, redeemList) = divideMissions(missions)
                    challengeList.initUnread(readIds)
                    return@map Result.success<Pair<List<Mission>, List<Mission>>, Error>(challengeList to redeemList)
                }
    }

    private fun parseRewardError(error: RewardServiceError): Error = when (error) {
        is RewardServiceError.NetworkError -> Error.NoConnectionError
        is RewardServiceError.MsrpDisabled,
        is RewardServiceError.Unauthorized,
        is RewardServiceError.Unknown -> Error.UnknownError
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
        forEach { it.unread = !readIds.contains(it.mid) }
    }

    suspend operator fun invoke(): LiveData<Result<Pair<List<Mission>, List<Mission>>, Error>> =
            resultLiveData.also { updateMissions() }

    private suspend fun updateMissions() {
        val userToken = userRepository.getUserToken()
        val missionsResult = missionRepository.getMissions(userToken)
        missionsLiveData.value = missionsResult
    }

    sealed class Error {
        object NoConnectionError : Error()
        object UnknownError : Error()
    }
}