package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RedeemServiceError
import org.mozilla.rocket.msrp.data.RewardCouponDoc
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.getNotNull

class RedeemUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(redeemUrl: String): Result<RewardCouponDoc, Error> {
        val userToken = userRepository.getUserToken()
        val rewardCouponDoc = missionRepository.redeem(userToken, redeemUrl).getNotNull { error ->
            when (error) {
                is RedeemServiceError.Failure,
                is RedeemServiceError.UsedUp,
                is RedeemServiceError.NotReady,
                is RedeemServiceError.InvalidRewardType,
                is RedeemServiceError.NotLogin -> Result.error<RewardCouponDoc, Error>(error = Error.UnknownError)
            }
        }

        return Result.success(rewardCouponDoc)
    }

    sealed class Error {
        object UnknownError : Error()
    }
}