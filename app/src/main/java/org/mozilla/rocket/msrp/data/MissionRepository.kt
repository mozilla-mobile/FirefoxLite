package org.mozilla.rocket.msrp.data

import android.util.Log
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import mozilla.components.concept.fetch.interceptor.withInterceptors
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.utils.FirebaseHelper
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
    open fun fetchMission(accessToken: String?): LoadMissionsResult {
        val token = accessToken
                ?: return LoadMissionsResult.Failure(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return LoadMissionsResult.Failure(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$missionListEndpoint?tz=${TimeZone.getDefault().id}"
        return sendRequest(
            request = Request(url = endpoint, headers = createHeader(token)),
            onSuccess = {
                parseMissionListResponse(it)
            },
            onError = {
                log("fetch mission failed, msg=${it.message}")
                LoadMissionsResult.Failure(error = RewardServiceError.Unknown(it.message.orEmpty()))
            }
        )
    }

    private fun parseMissionListResponse(response: Response): LoadMissionsResult {
        return when (response.status) {
            400 -> LoadMissionsResult.Failure(error = RewardServiceError.Unauthorized)
            200 -> {
                val json = response.body.string()
                log("response=$json")

                val missions = convertToMissionList(json)
                log("mission list=$missions")

                LoadMissionsResult.Success(missions)
            }
            else -> return LoadMissionsResult.Failure(error = RewardServiceError.Unknown(response.body.string()))
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
                title = missionJson.optString("title"),
                description = missionJson.optString("description"),
                endpoint = missionJson.optString("endpoint"),
                events = interestEvents,
                important = missionJson.optBoolean("important"),
                status = missionJson.optInt("status"),
                minVersion = missionJson.optInt("minVersion"),
                missionType = missionJson.optString("missionType"),
                missionProgress = progress,
                redeem = ""
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
    fun joinMission(mission: Mission, accessToken: String?): JoinMissionResult {
        val token = accessToken
                ?: return JoinMissionResult.Failure(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return JoinMissionResult.Failure(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST/missions${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.POST),
                onSuccess = {
                    parseJoinMissionResponse(it)
                },
                onError = {
                    log("join mission failed, msg=${it.message}")
                    JoinMissionResult.Failure(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseJoinMissionResponse(response: Response): JoinMissionResult {
        val body = response.body.string()
        val root = JSONObject(body)

        log("join response, code=${response.status}, body=$body")

        return when (response.status) {
            200 -> {
                val result = root.optJSONObject("result")
                val mid = result.optString("mid")

                val status = MissionJoinStatus.valueOf(result.optInt("status"))
                status ?: return JoinMissionResult.Failure("unknown join status=$status")

                JoinMissionResult.Success(mid, status)
            }

            else -> {
                val msg = root.optString("message")
                JoinMissionResult.Failure("join failed, code=${response.status}, msg=$msg")
            }
        }
    }

    /**
     * Check-in missions that is interested in the given ping
     */
    fun checkInMission(ping: String, accessToken: String?): CheckInMissionResult {
        val token = accessToken
                ?: return CheckInMissionResult.Failure(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return CheckInMissionResult.Failure(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST/ping/$ping?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.PUT),
                onSuccess = {
                    parseCheckInMissionResponse(it)
                },
                onError = {
                    log("check-in mission failed, msg=${it.message}")
                    CheckInMissionResult.Failure(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseCheckInMissionResponse(response: Response): CheckInMissionResult {
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

                return CheckInMissionResult.Success(missions)
            }

            else -> {
                val msg = root.optString("message")
                CheckInMissionResult.Failure("check-in failed, code=${response.status}, msg=$msg")
            }
        }
    }

    fun quitMission(mission: Mission, accessToken: String?): QuitMissionResult {
        val token = accessToken
                ?: return QuitMissionResult.Failure(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return QuitMissionResult.Failure(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$MSRP_HOST/missions${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.DELETE),
                onSuccess = {
                    parseQuitMissionResponse(it)
                },
                onError = {
                    log("join mission failed, msg=${it.message}")
                    QuitMissionResult.Failure(error = RewardServiceError.Unknown(it.message.orEmpty()))
                }
        )
    }

    private fun parseQuitMissionResponse(response: Response): QuitMissionResult {
        val body = response.body.string()
        val root = JSONObject(body)

        log("quit response, code=${response.status}, body=$body")

        return when (response.status) {
            200 -> {
                val mid = root.optString("mid")

                val status = MissionJoinStatus.valueOf(root.optInt("status"))
                status ?: return QuitMissionResult.Failure("unknown join status=$status")

                QuitMissionResult.Success(mid, status)
            }

            else -> {
                val msg = root.optString("message")
                QuitMissionResult.Failure("quit failed, code=${response.status}, msg=$msg")
            }
        }
    }

    fun redeem(userToken: String?, redeemUrl: String): RedeemResult {

        if (userToken == null) {
            return RedeemResult.NotLogin("Please login first")
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
                return when {
                    response.status == 500 -> { // 500 is define in the server spec...in the future.
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        RedeemResult.Failure(message) // return failure with message
                    }
                    response.status == 400 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        RedeemResult.InvalidRewardType(message) // return failure with message
                    }
                    response.status == 403 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        RedeemResult.NotReady(message) // return failure with message
                    }
                    response.status == 404 -> {
                        val resJson = JSONObject(response.body.string())
                        val message = resJson.optString("message")
                        RedeemResult.UsedUp(message) // return failure with message
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

                        RedeemResult.Success(reward) // return failure with message
                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            throw RuntimeException("Should not reach this")
                        } else {
                            RedeemResult.Failure("Something is wrong")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Redeem error $e")
            return RedeemResult.Failure("Something is wrong")
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
        private const val MSRP_HOST = "https://rocket-dev01.appspot.com/api/v1/"
    }
}

/**
 * Force the client to handle possible exceptions
 * */
sealed class RewardServiceException : RuntimeException() {
    class ServerErrorException : RewardServiceException()
    class AuthorizationException : RewardServiceException()
}

@Suppress("UNUSED_PARAMETER")
sealed class RewardServiceError {

    object MsrpDisabled : RewardServiceError()
    object Unauthorized : RewardServiceError()
    class Unknown(msg: String) : RewardServiceError()
}

@Suppress("UNUSED_PARAMETER")
sealed class LoadMissionsResult {
    class Success(val missions: List<Mission>) : LoadMissionsResult()
    class Failure(val message: String = "", error: RewardServiceError? = null) : LoadMissionsResult()
}

@Suppress("UNUSED_PARAMETER")
sealed class JoinMissionResult {
    class Success(val mid: String, val status: MissionJoinStatus) : JoinMissionResult()
    class Failure(val message: String = "", val error: RewardServiceError? = null) : JoinMissionResult()
}

@Suppress("UNUSED_PARAMETER")
sealed class CheckInMissionResult {
    class Success(val missions: List<CheckedInMission>) : CheckInMissionResult()
    class Failure(val message: String = "", val error: RewardServiceError? = null) : CheckInMissionResult()
}

@Suppress("UNUSED_PARAMETER")
sealed class QuitMissionResult {
    class Success(mis: String, status: MissionJoinStatus) : QuitMissionResult()
    class Failure(val message: String = "", val error: RewardServiceError? = null) : QuitMissionResult()
}

data class CheckedInMission(
    val mid: String,
    val missionType: MissionType,
    val progress: MissionProgress
)

/**
 * copy from backend code
 * TODO: share the code in the future.
 * */
sealed class RedeemResult {
    class Success(val rewardCouponDoc: RewardCouponDoc) : RedeemResult()
    class UsedUp(val message: String) : RedeemResult()
    class NotReady(val message: String) : RedeemResult()
    class Failure(val message: String) : RedeemResult()
    class InvalidRewardType(val message: String) : RedeemResult()
    class NotLogin(val message: String) : RedeemResult()
}

class RedeemResponse(var rewardCouponDoc: RewardCouponDoc)

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
