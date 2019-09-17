package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RedeemResult
import org.mozilla.rocket.msrp.data.UserRepository

class RedeemUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) : UseCase<RedeemRequest, RedeemResult>() {

    override suspend fun execute(parameters: RedeemRequest): RedeemResult {
        return missionRepository.redeem(userRepository.getUserToken(), parameters.redeemUrl)
    }
}

class RedeemRequest(val redeemUrl: String)