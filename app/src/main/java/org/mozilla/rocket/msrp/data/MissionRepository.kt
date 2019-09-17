package org.mozilla.rocket.msrp.data

import android.util.Log
import mozilla.components.concept.fetch.BuildConfig
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.interceptor.withInterceptors
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.json.JSONObject
import java.lang.Exception
import java.util.Random
import java.util.TimeZone

open class MissionRepository {

    /**
     * Fetch a list of [Mission]s from the server
     *
     * @param [missionGroupURI] the mission group endpoint
     * @return a list of [Mission]s the user can join or has joined
     * @throws RewardServiceException.ServerErrorException when there's a server error
     * @throws RewardServiceException.AuthorizationException when there's a something wrong with authorization
     */
    open fun fetchMission(missionGroupURI: String): List<Mission> {

        // pretending we are doing some network request...
        val fakeUrl = "http://rocket-dev01.appspot.com/health"
        val request = Request(
            url = fakeUrl,
            headers = MutableHeaders(
                "Accept" to "application/json; q=0.5",
                "Accept" to "application/vnd.github.v3+json"
//                , "Authorization" to "Bearer SOME-JWT" // add this when we do the real integration
            )
        )
        // pretending we are doing some network request here...
        // since we only have one data source, we'll just do it in the repository.
        HttpURLConnectionClient().withInterceptors(LoggingInterceptor()).fetch(request).use { response ->
            when {
                response.status >= 500 -> throw RewardServiceException.ServerErrorException()
                response.status >= 400 -> throw RewardServiceException.AuthorizationException()
                Random().nextInt(10) > 8 -> // fake failure case
                    return listOf()
                // FIXME: use real data from the server
                else -> return listOf(
                    Mission(
                        mid = "000001",
                        title = "Daily Mission 1",
                        description = "Click vertical everyday",
                        expireDate = System.currentTimeMillis(),

                        events = listOf("CLICK_PANEL_PIN_TOP_SITE"),

                        important = true,
                        status = 0, // 0: new , 1: joined 2. redeem,
                        endpoint = "/v1/daily_mission/xxdase-eadsad",
                        redeem = "/v1/redeem/asdsa-esadsa=das-dased-sadas",

                        missionProgress = MissionProgress.TypeDaily(
                            joinDate = null,
                            currentDay = null,
                            totalDays = 10
                        )
                    )
                )
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

    companion object {
        private const val TAG = "MissionRepository"
    }
}

/**
 * Force the client to handle possible exceptions
 * */
sealed class RewardServiceException : RuntimeException() {
    class ServerErrorException : RewardServiceException()
    class AuthorizationException : RewardServiceException()
}

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

class RedeemResponaw(var rewardCouponDoc: RewardCouponDoc)

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
