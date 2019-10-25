package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionCoupon
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RedeemServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.map

class GetCouponUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(mission: Mission): Result<MissionCoupon, Error> {
        val userToken = userRepository.getUserToken()
        return missionRepository.getCoupon(userToken, mission.mid, requireNotNull(mission.redeem)).map(
            transformResult = { it },
            transformError = {
                return@map when (it) {
                    is RedeemServiceError.NetworkError -> Error.NetworkError
                    is RedeemServiceError.Failure,
                    is RedeemServiceError.UsedUp,
                    is RedeemServiceError.NotReady,
                    is RedeemServiceError.InvalidRewardType,
                    is RedeemServiceError.NotLogin -> Error.UnknownError
                }
            }
        )
    }

    sealed class Error {
        object NetworkError : Error()
        object UnknownError : Error()
    }
}