package org.mozilla.rocket.msrp.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import mozilla.components.concept.fetch.interceptor.withInterceptors
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.Result
import java.util.TimeZone

open class MissionRepository {

    private val missionListEndpoint: String
        get() = FirebaseHelper.getFirebase().getRcString(STR_RC_MISSION_LIST_ENDPOINT)

    private val isMsrpEnabled: Boolean
        get() = FirebaseHelper.getFirebase().getRcBoolean(BOOL_RC_MSRP_ENABLED)

    fun isMsrpAvailable(): Boolean {
        return isMsrpEnabled && missionListEndpoint.isNotBlank()
    }

    /**
     * Fetch a list of [Mission]s from the server
     *
     * @return a list of [Mission]s the user can join or has joined
     */
    open suspend fun fetchMission(accessToken: String?): Result<List<Mission>, RewardServiceError> = withContext(Dispatchers.IO) {
        val token = accessToken
                ?: return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$missionListEndpoint?tz=${TimeZone.getDefault().id}"
        return@withContext sendRequest(
            request = Request(url = endpoint, headers = createHeader(token)),
            onSuccess = {
                parseMissionListResponse(it)
            },
            onError = {
                log("fetch mission failed, msg=${it.message}")
                Result.error(error = RewardServiceError.Unknown(it.message.orEmpty()))
            }
        )
    }

    private fun parseMissionListResponse(response: Response): Result<List<Mission>, RewardServiceError> {
        return when (response.status) {
            400 -> Result.error(error = RewardServiceError.Unauthorized)
            200 -> {
                val json = response.body.string()
                log("response=$json")

                val missions = convertToMissionList(json)
                log("mission list=$missions")

                Result.success(missions)
            }
            else -> Result.error(error = RewardServiceError.Unknown(response.body.string()))
        }
    }

    private fun convertToMissionList(json: String): List<Mission> {
        val root = JSONObject(json)
        val missionArray = root.getJSONArray("result")

        return (0 until missionArray.length()).mapNotNull {
            val missionJson = missionArray.getJSONObject(it)

            val interestEvents = parseInterestEvents(missionJson.optJSONArray("events"))

            val missionType = MissionType.valueOf(missionJson.optString("missionType"))
                    ?: return@mapNotNull null

            val progress = parseProgress(missionType, missionJson.optJSONObject("progress"))
                    ?: return@mapNotNull null

            Mission(
                mid = missionJson.optString("mid"),
                missionType = missionJson.optString("missionType"),
                title = missionJson.optString("title"),
                description = missionJson.optString("description"),
                imageUrl = missionJson.optString("imageUrl"),
                endpoint = missionJson.optString("joinEndpoint"),
                redeem = missionJson.optString("redeemEndpoint"),
                events = interestEvents,
                important = missionJson.optBoolean("important"),
                minVersion = missionJson.optInt("minVersion"),
                joinEndDate = missionJson.optLong("joinEndDate"),
                expiredDate = missionJson.optLong("expiredDate"),
                status = missionJson.optInt("status"),
                missionProgress = progress
            )
        }
    }

    private fun parseInterestEvents(events: JSONArray): List<String> {
        return (0 until events.length()).map { events.optString(it) }
    }

    private fun parseProgress(missionType: MissionType?, progress: JSONObject): MissionProgress? {
        return when (missionType) {
            is MissionType.MissionDaily -> MissionProgress.TypeDaily(
                    joinDate = progress.optLong("joinDate"),
                    currentDay = progress.optInt("currentDayCount"),
                    totalDays = progress.optInt("totalDays"),
                    message = progress.optString("message")
            )

            else -> null
        }
    }

    /**
     * Join to the given mission
     */
    fun joinMission(mission: Mission, accessToken: String?): Result<JoinMissionResult, RewardServiceError> {
        val token = accessToken
                ?: return Result.error(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return Result.error(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.POST),
                onSuccess = {
                    parseJoinMissionResponse(it)
                },
                onError = {
                    log("join mission failed, msg=${it.message}")
                    Result.error(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseJoinMissionResponse(response: Response): Result<JoinMissionResult, RewardServiceError> {
        val body = response.body.string()
        val root = JSONObject(body)

        log("join response, code=${response.status}, body=$body")

        return when (response.status) {
            200 -> {
                val result = root.optJSONObject("result")
                val mid = result.optString("mid")

                val status = MissionJoinStatus.valueOf(result.optInt("status"))
                status ?: return Result.error("unknown join status=$status")

                Result.success(JoinMissionResult(mid, status))
            }

            else -> {
                val msg = root.optString("message")
                Result.error("join failed, code=${response.status}, msg=$msg")
            }
        }
    }

    /**
     * Check-in missions that is interested in the given ping
     */
    fun checkInMission(ping: String, accessToken: String?): Result<List<CheckedInMission>, RewardServiceError> {
        val token = accessToken
                ?: return Result.error(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return Result.error(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST/api/v1/ping/$ping?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.PUT),
                onSuccess = {
                    parseCheckInMissionResponse(it)
                },
                onError = {
                    log("check-in mission failed, msg=${it.message}")
                    Result.error(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseCheckInMissionResponse(response: Response): Result<List<CheckedInMission>, RewardServiceError> {
        val body = response.body.string()
        val root = JSONObject(body)

        log("check-in response, code=${response.status}, body=$body")

        return when (response.status) {
            200 -> {
                val missionArray = root.optJSONArray("result")
                val missions = (0 until missionArray.length()).mapNotNull {
                    val missionJson = missionArray.getJSONObject(it)
                    val missionType = MissionType.valueOf(missionJson.optString("missionType"))
                            ?: return@mapNotNull null

                    val progress = parseProgress(missionType, missionJson.optJSONObject("progress"))
                            ?: return@mapNotNull null

                    CheckedInMission(
                            mid = missionJson.optString("mid"),
                            missionType = missionType,
                            progress = progress
                    )
                }

                return Result.success(missions)
            }

            else -> {
                val msg = root.optString("message")
                Result.error("check-in failed, code=${response.status}, msg=$msg")
            }
        }
    }

    fun quitMission(mission: Mission, accessToken: String?): Result<QuitMissionResult, RewardServiceError> {
        val token = accessToken
                ?: return Result.error(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return Result.error(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.DELETE),
                onSuccess = {
                    parseQuitMissionResponse(it)
                },
                onError = {
                    log("join mission failed, msg=${it.message}")
                    Result.error(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseQuitMissionResponse(response: Response): Result<QuitMissionResult, RewardServiceError> {
        val body = response.body.string()
        val root = JSONObject(body)

        log("quit response, code=${response.status}, body=$body")

        return when (response.status) {
            200 -> {
                val mid = root.optString("mid")

                val status = MissionJoinStatus.valueOf(root.optInt("status"))
                status ?: return Result.error("unknown join status=$status")

                Result.success(QuitMissionResult(mid, status))
            }

            else -> {
                val msg = root.optString("message")
                Result.error("quit failed, code=${response.status}, msg=$msg")
            }
        }
    }

    suspend fun redeem(userToken: String?, redeemUrl: String): Result<RewardCouponDoc, RedeemServiceError> = withContext(Dispatchers.IO) {

        if (userToken == null) {
            return@withContext Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.NotLogin("Please login first"))
        }
        val request = Request(
            url = redeemUrl,
            headers = MutableHeaders(
                "Accept" to "application/json; q=0.5",
                "Accept" to "application/vnd.github.v3+json",
                "tz" to TimeZone.getDefault().id, // too bad we can't use ZoneId(require API 26)
                "Authorization" to "Bearer $userToken" // add this when we do the real integration)
            )
        )
        try {

            // pretending we are doing some network request here...
            // since we only have one data source, we'll just do it in the repository.
            HttpURLConnectionClient().withInterceptors(LoggingInterceptor()).fetch(request).use { response ->
                return@withContext when {
                    response.status == 500 -> { // 500 is define in the server spec...in the future.
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.Failure(message)) // return failure with message
                    }
                    response.status == 400 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.InvalidRewardType(message)) // return failure with message
                    }
                    response.status == 403 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.NotReady(message)) // return failure with message
                    }
                    response.status == 404 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.UsedUp(message)) // return failure with message
                    }
                    response.status == 200 -> {
                        val responseStr = response.body.string()
                        Log.e(TAG, "Redeem responseStr: $responseStr")
                        val resJson = JSONObject(responseStr).getJSONObject("rewardCoupon")
                        val reward = RewardCouponDoc(
                            resJson.optString("rid"),
                            resJson.optString("uid"),
                            resJson.optString("mid"),
                            resJson.optString("code"),
                            resJson.optString("campaign"),
                            resJson.optString("title"),
                            resJson.optString("content"),
                            resJson.optLong("expire_date"),
                            resJson.optLong("created_timestamp"),
                            resJson.optLong("updated_timestamp")
                        )

                        Result.success(reward) // return failure with message
                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            throw RuntimeException("Should not reach this")
                        } else {
                            Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.Failure("Something is wrong"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Redeem error $e")
            return@withContext Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.Failure("Something is wrong"))
        }
    }

    private fun createHeader(token: String) = MutableHeaders(
            "Accept" to "application/json",
            "tz" to TimeZone.getDefault().id,
            "Authorization" to "Bearer $token"
    )

    private fun <T> sendRequest(request: Request, onSuccess: (Response) -> T, onError: (Exception) -> T): T {
        return try {
            return HttpURLConnectionClient()
                    .withInterceptors(LoggingInterceptor())
                    .fetch(request)
                    .use { onSuccess(it) }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "MissionRepository"
        private const val BOOL_RC_MSRP_ENABLED = "bool_msrp_enabled"
        private const val STR_RC_MISSION_LIST_ENDPOINT = "str_mission_list_endpoint"

        // TODO: Update host url
        private const val MSRP_HOST = "https://rocket-dev01.appspot.com"
    }
}

@Suppress("UNUSED_PARAMETER")
sealed class RewardServiceError {
    object MsrpDisabled : RewardServiceError()
    object Unauthorized : RewardServiceError()
    class Unknown(msg: String) : RewardServiceError()
}

@Suppress("UNUSED_PARAMETER")
class JoinMissionResult(val mid: String, val status: MissionJoinStatus)

@Suppress("UNUSED_PARAMETER")
class QuitMissionResult(val mid: String, val status: MissionJoinStatus)

data class CheckedInMission(
    val mid: String,
    val missionType: MissionType,
    val progress: MissionProgress
)

sealed class RedeemServiceError {
    class UsedUp(val message: String) : RedeemServiceError()
    class NotReady(val message: String) : RedeemServiceError()
    class Failure(val message: String) : RedeemServiceError()
    class InvalidRewardType(val message: String) : RedeemServiceError()
    class NotLogin(val message: String) : RedeemServiceError()
}

class RewardCouponDoc(
    var rid: String? = null,
    var uid: String? = null,
    var mid: String? = null,
    var code: String? = null,
    var campaign: String? = null,
    var title: String? = null,
    var content: String? = null,
    var expire_date: Long? = null,
    var created_timestamp: Long? = null,
    var updated_timestamp: Long? = null
)

sealed class MissionType {

    object MissionDaily : MissionType()

    companion object {
        fun valueOf(missionType: String): MissionType? {
            return when (missionType) {
                "mission_daily" -> MissionDaily
                else -> null
            }
        }
    }
}

sealed class MissionJoinStatus {
    object New : MissionJoinStatus()
    object Joined : MissionJoinStatus()
    object Completed : MissionJoinStatus()
    object Redeemed : MissionJoinStatus()

    companion object {
        fun valueOf(value: Int): MissionJoinStatus? {
            return when (value) {
                0 -> New
                1 -> Joined
                2 -> Completed
                3 -> Redeemed
                else -> null
            }
        }
    }
}
