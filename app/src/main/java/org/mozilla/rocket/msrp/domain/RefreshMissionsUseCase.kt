package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.map

class RefreshMissionsUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(): Result<List<Mission>, Error> {
        val userToken = userRepository.getUserToken()
        return missionRepository.refreshMissions(userToken).map(
            transformResult = { it },
            transformError = {
                when (it) {
                    is RewardServiceError.AccountDisabled -> Error.AccountDisabled
                    is RewardServiceError.NetworkError -> Error.NetworkError
                    is RewardServiceError.MsrpDisabled,
                    is RewardServiceError.NoQuota,
                    is RewardServiceError.Unauthorized,
                    is RewardServiceError.Unknown -> Error.UnknownError
                }
            }
        )
    }

    sealed class Error {
        object AccountDisabled : Error()
        object NetworkError : Error()
        object UnknownError : Error()
    }
}