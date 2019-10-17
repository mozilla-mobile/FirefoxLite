package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.map

class JoinMissionUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(mission: Mission): Result<Unit, Error> {
        val userToken = userRepository.getUserToken()
        return missionRepository.joinMission(userToken, mission).map(
            transformResult = { Unit },
            transformError = {
                when (it) {
                    is RewardServiceError.AccountDisabled -> Error.AccountDisabled
                    is RewardServiceError.NoQuota -> Error.NoQuota
                    is RewardServiceError.NetworkError -> Error.NetworkError
                    is RewardServiceError.MsrpDisabled,
                    is RewardServiceError.Unauthorized,
                    is RewardServiceError.Unknown -> Error.UnknownError
                }
            }
        )
    }

    sealed class Error {
        object AccountDisabled : Error()
        object NoQuota : Error()
        object NetworkError : Error()
        object UnknownError : Error()
    }
}