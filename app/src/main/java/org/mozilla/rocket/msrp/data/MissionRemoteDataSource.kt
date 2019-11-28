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
import java.io.IOException
import java.util.Locale
import java.util.TimeZone

class MissionRemoteDataSource {

    private val msrpApiHost: String
        get() = FirebaseHelper.getFirebase().getRcString(STR_RC_MSRP_API_HOST)

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
    suspend fun getMissions(accessToken: String?): Result<List<Mission>, RewardServiceError> = withContext(Dispatchers.IO) {
        val token = accessToken
                ?: return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$msrpApiHost$missionListEndpoint?tz=${TimeZone.getDefault().id}"
        return@withContext sendRequest(
                request = Request(url = endpoint, headers = createHeader(token)),
                onSuccess = {
                    parseMissionListResponse(it)
                },
                onError = {
                    log("fetch mission failed, msg=$it")
                    Result.error(error = RewardServiceError.NetworkError)
                }
        )
    }

    private fun parseMissionListResponse(response: Response): Result<List<Mission>, RewardServiceError> {
        return when (response.status) {
            400 -> Result.error(error = RewardServiceError.Unauthorized)
            403 -> Result.error(error = RewardServiceError.AccountDisabled)
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

            val parameters = missionJson.optJSONObject("parameters")

            Mission(
                mid = missionJson.optString("mid"),
                missionType = missionJson.optString("missionType"),
                title = missionJson.optString("title"),
                missionName = missionJson.optString("missionName"),
                description = missionJson.optString("description"),
                imageUrl = missionJson.optString("imageUrl"),
                endpoint = missionJson.optString("joinEndpoint"),
                redeem = missionJson.optString("redeemEndpoint"),
                events = interestEvents,
                important = missionJson.optBoolean("important"),
                minVersion = missionJson.optInt("minVersion"),
                minVerDialogTitle = missionJson.optString("minVerDialogTitle"),
                minVerDialogMessage = missionJson.optString("minVerDialogMessage"),
                minVerDialogImage = missionJson.optString("minVerDialogImage"),
                joinEndDate = missionJson.optLong("joinEndDate"),
                expiredDate = missionJson.optLong("expiredDate"),
                redeemEndDate = missionJson.optLong("redeemEndDate"),
                rewardExpiredDate = missionJson.optLong("rewardExpiredDate"),
                status = missionJson.optInt("status"),
                missionProgress = progress,
                totalDays = parameters?.optInt("totalDays") ?: 0
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
    suspend fun joinMission(mission: Mission, accessToken: String?): Result<JoinMissionResult, RewardServiceError> = withContext(Dispatchers.IO) {
        val token = accessToken
                ?: return@withContext Result.error<JoinMissionResult, RewardServiceError>(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return@withContext Result.error<JoinMissionResult, RewardServiceError>(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "${msrpApiHost}${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return@withContext sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.POST),
                onSuccess = {
                    parseJoinMissionResponse(it)
                },
                onError = {
                    log("join mission failed, msg=$it")
                    Result.error(error = RewardServiceError.NetworkError)
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
                status ?: return Result.error("unknown join status=$status", error = RewardServiceError.Unknown("unknown join status=$status"))

                Result.success(JoinMissionResult(mid, status))
            }
            403 -> {
                if (body.isEmpty()) {
                    Result.error<JoinMissionResult, RewardServiceError>(error = RewardServiceError.AccountDisabled)
                } else {
                    when (val errorType = ErrorType.fromJson(body)) {
                        is ErrorType.Unknown -> Result.error<JoinMissionResult, RewardServiceError>(error = RewardServiceError.Unknown(errorType.message))
                        is ErrorType.NoQuota -> Result.error<JoinMissionResult, RewardServiceError>(error = RewardServiceError.NoQuota)
                    }
                }
            }
            else -> {
                val msg = root.optString("message")
                Result.error("join failed, code=${response.status}, msg=$msg", error = RewardServiceError.Unknown(msg))
            }
        }
    }

    /**
     * Check-in missions that is interested in the given ping
     */
    suspend fun checkInMission(ping: String, accessToken: String?): Result<List<Mission>, RewardServiceError> = withContext(Dispatchers.IO) {
        val token = accessToken
                ?: return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return@withContext Result.error<List<Mission>, RewardServiceError>(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "$msrpApiHost/$MSRP_API_VERSION_PATH/ping/$ping?tz=${TimeZone.getDefault().id}"
        return@withContext sendRequest(
                request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.PUT),
                onSuccess = {
                    parseMissionListResponse(it)
                },
                onError = {
                    log("check-in mission failed, msg=$it")
                    Result.error(error = RewardServiceError.NetworkError)
                }
        )
    }

    suspend fun quitMission(mission: Mission, accessToken: String?): Result<QuitMissionResult, RewardServiceError> = withContext(Dispatchers.IO) {
        val token = accessToken
                ?: return@withContext Result.error<QuitMissionResult, RewardServiceError>(error = RewardServiceError.Unauthorized)

        if (!isMsrpAvailable()) {
            return@withContext Result.error<QuitMissionResult, RewardServiceError>(error = RewardServiceError.MsrpDisabled)
        }

        val endpoint = "${msrpApiHost}${mission.endpoint}?tz=${TimeZone.getDefault().id}"
        return@withContext sendRequest(
            request = Request(url = endpoint, headers = createHeader(token), method = Request.Method.DELETE),
            onSuccess = {
                parseQuitMissionResponse(it)
            },
            onError = {
                log("join mission failed, msg=$it")
                Result.error(error = RewardServiceError.NetworkError)
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
                status ?: return Result.error("unknown join status=$status", error = RewardServiceError.Unknown("unknown join status=$status"))

                Result.success(QuitMissionResult(mid, status))
            }

            else -> {
                val msg = root.optString("message")
                Result.error("quit failed, code=${response.status}, msg=$msg", error = RewardServiceError.Unknown(msg))
            }
        }
    }

    suspend fun redeem(userToken: String?, redeemEndPoint: String): Result<RewardCouponDoc, RedeemServiceError> = withContext(Dispatchers.IO) {

        if (userToken == null) {
            return@withContext Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.NotLogin("Please login first"))
        }
        val hasParams = redeemEndPoint.contains("?")
        val concatSymbol = if (hasParams) {
            "&"
        } else {
            "?"
        }
        val url = "${msrpApiHost}${redeemEndPoint}${concatSymbol}tz=${TimeZone.getDefault().id}"
        val request = Request(
                url = url,
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
                            resJson.optLong("updated_timestamp"),
                            resJson.optString("open_link")
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
        } catch (e: IOException) {
            log("Redeem network error, msg=$e")
            return@withContext Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.NetworkError)
        } catch (e: Exception) {
            Log.e(TAG, "Redeem error $e")
            return@withContext Result.error<RewardCouponDoc, RedeemServiceError>(error = RedeemServiceError.Failure("Something is wrong"))
        }
    }

    private fun createHeader(token: String) = MutableHeaders(
            "Accept" to "application/json",
            "tz" to TimeZone.getDefault().id,
            "Authorization" to "Bearer $token",
            "Accept-Language" to Locale.getDefault().toLanguageTag()
    )

    private fun <T> sendRequest(request: Request, onSuccess: (Response) -> T, onError: (Exception) -> T): T {
        return try {
            return HttpURLConnectionClient()
                    .withInterceptors(LoggingInterceptor())
                    .fetch(request)
                    .use { onSuccess(it) }
        } catch (e: IOException) {
            onError(e)
        }
    }

    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "MissionRemoteDataSource"
        private const val BOOL_RC_MSRP_ENABLED = "bool_msrp_enabled"
        private const val STR_RC_MISSION_LIST_ENDPOINT = "str_mission_list_endpoint"
        private const val STR_RC_MSRP_API_HOST = "str_msrp_api_host"

        // TODO: to be updated
        private const val MSRP_API_VERSION_PATH = "api/v1/"
    }
}

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

class JoinMissionResult(val mid: String, val status: MissionJoinStatus)

class QuitMissionResult(val mid: String, val status: MissionJoinStatus)

class RewardCouponDoc(
    var rid: String,
    var uid: String,
    var mid: String,
    var code: String,
    var campaign: String,
    var title: String,
    var content: String,
    var expire_date: Long,
    var created_timestamp: Long,
    var updated_timestamp: Long,
    var open_link: String
)

sealed class RewardServiceError {
    object NetworkError : RewardServiceError()
    object MsrpDisabled : RewardServiceError()
    object Unauthorized : RewardServiceError()
    object AccountDisabled : RewardServiceError()
    object NoQuota : RewardServiceError()
    class Unknown(val msg: String) : RewardServiceError()
}

sealed class RedeemServiceError {
    object NetworkError : RedeemServiceError()
    class UsedUp(val message: String) : RedeemServiceError()
    class NotReady(val message: String) : RedeemServiceError()
    class Failure(val message: String) : RedeemServiceError()
    class InvalidRewardType(val message: String) : RedeemServiceError()
    class NotLogin(val message: String) : RedeemServiceError()
}

private data class ErrorData(
    val message: String,
    val reason: Int
) {
    companion object {
        fun fromJson(jsonStr: String): ErrorData {
            val json = JSONObject(jsonStr)
            return ErrorData(
                message = json.optString("message"),
                reason = json.optInt("reason", 0)
            )
        }
    }
}

sealed class ErrorType(val message: String) {
    class Unknown(message: String) : ErrorType(message)
    class NoQuota(message: String) : ErrorType(message)

    companion object {
        private const val ERROR_CODE_NO_QUOTA = 4

        fun fromJson(jsonStr: String): ErrorType {
            val errorData = ErrorData.fromJson(jsonStr)
            return when (errorData.reason) {
                ERROR_CODE_NO_QUOTA -> NoQuota(errorData.message)
                else -> Unknown(errorData.message)
            }
        }
    }
}