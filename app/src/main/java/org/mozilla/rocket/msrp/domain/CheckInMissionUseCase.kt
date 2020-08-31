package org.mozilla.rocket.msrp.domain

import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RewardServiceError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.map

class CheckInMissionUseCase(
    private val missionRepository: MissionRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(pingType: PingType): Result<Data?, Error> {
        val userToken = userRepository.getUserToken()
        return missionRepository.checkInMission(userToken, pingType.ping).map(
            transformResult = { missions ->
                if (missions.isEmpty()) {
                    return@map null
                }
                val firstMission = missions.first()
                return@map when (val progress = firstMission.missionProgress ?: error("missionProgress should not be null")) {
                    is MissionProgress.TypeDaily -> {
                        val isCompleted = progress.isCompleted()
                        // hard fix in client
                        val mission = if (isCompleted) {
                            firstMission.copy(status = Mission.STATUS_REDEEMABLE)
                        } else {
                            firstMission
                        }
                        Data(mission, isCompleted)
                    }
                }
            },
            transformError = {
                when (it) {
                    is RewardServiceError.NetworkError -> Error.NetworkError
                    is RewardServiceError.AccountDisabled -> Error.AccountDisabled
                    is RewardServiceError.MsrpDisabled,
                    is RewardServiceError.NoQuota,
                    is RewardServiceError.Unauthorized,
                    is RewardServiceError.Unknown -> Error.UnknownError
                }
            }
        )
    }

    private fun MissionProgress.TypeDaily.isCompleted(): Boolean =
            totalDays == currentDay

    data class Data(
        val mission: Mission,
        val hasMissionCompleted: Boolean
    )

    sealed class Error {
        object AccountDisabled : Error()
        object NetworkError : Error()
        object UnknownError : Error()
    }

    sealed class PingType(val ping: String) {
        class Shopping : PingType(PING_ENTER_SHOPPING)
        class Game : PingType(PING_ENTER_GAME)
        class Travel : PingType(PING_ENTER_TRAVEL)
        class Lifestyle : PingType(PING_ENTER_LIFESTYLE)
    }

    companion object {
        // TODO: hard code temporarily
        private const val PING_ENTER_SHOPPING = "${TelemetryWrapper.Category.ACTION}-${TelemetryWrapper.Method.CLICK}-${TelemetryWrapper.Object.CONTENT_HUB}-${""}" +
                ":${TelemetryWrapper.Extra_Value.SHOPPING}"
        private const val PING_ENTER_GAME = "${TelemetryWrapper.Category.ACTION}-${TelemetryWrapper.Method.CLICK}-${TelemetryWrapper.Object.CONTENT_HUB}-${""}" +
                ":"
        private const val PING_ENTER_TRAVEL = "${TelemetryWrapper.Category.ACTION}-${TelemetryWrapper.Method.CLICK}-${TelemetryWrapper.Object.CONTENT_HUB}-${""}" +
                ":${TelemetryWrapper.Extra_Value.TRAVEL}"
        private const val PING_ENTER_LIFESTYLE = "${TelemetryWrapper.Category.ACTION}-${TelemetryWrapper.Method.CLICK}-${TelemetryWrapper.Object.CONTENT_HUB}-${""}" +
                ":${TelemetryWrapper.Extra_Value.LIFESTYLE}"
    }
}