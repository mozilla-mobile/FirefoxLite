package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.data.UserServiceError
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.getNotNull

class LoadMissionsUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) : UseCase<LoadMissionsUseCaseParameter, Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>>() {

    override suspend fun execute(parameters: LoadMissionsUseCaseParameter): Result<Pair<List<Mission>, List<Mission>>, Error> {
        val userToken = userRepository.getUserToken().getNotNull { error ->
            return when (error) {
                is UserServiceError.GetUserTokenError -> Result.error(error = Error.UnknownError)
            }
        }
        val missions = missionRepository.fetchMission(userToken).getNotNull { error ->
            return when (error) {
                is RewardServiceError.MsrpDisabled,
                is RewardServiceError.Unauthorized,
                is RewardServiceError.Unknown -> Result.error(error = Error.UnknownError)
            }
        }
        val (challengeList, redeemList) = divideMissions(missions)

        return Result.success(challengeList to redeemList)
    }

    private fun divideMissions(missions: List<Mission>): Pair<List<Mission>, List<Mission>> {
        val map = missions.groupBy {
            it.status == Mission.STATUS_NEW || it.status == Mission.STATUS_JOINED
        }
        val challengeList = map[true] ?: emptyList()
        val redeemList = map[false] ?: emptyList()
        return challengeList to redeemList
    }

    sealed class Error {
        object NoConnectionError : Error()
        object UnknownError : Error()
    }
}

class LoadMissionsUseCaseParameter