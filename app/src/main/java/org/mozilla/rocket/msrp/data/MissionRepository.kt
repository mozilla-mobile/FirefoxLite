package org.mozilla.rocket.msrp.data

import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import java.util.Random

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
        val fakeUrl = "https://api.github.com/repos/mozilla-mobile/android-components/issues"
        val request = Request(
            url = fakeUrl,
            headers = MutableHeaders(
                "Accept" to "application/json; q=0.5",
                "Accept" to "application/vnd.github.v3+json",
                "Authorization" to "Bearer SOME-JWT"
            )
        )
        // pretending we are doing some network request here...
        // since we only have one data source, we'll just do it in the repository.
        HttpURLConnectionClient().fetch(request).use { response ->
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
}

/**
 * Force the client to handle possible exceptions
 * */
sealed class RewardServiceException : RuntimeException() {
    class ServerErrorException : RewardServiceException()
    class AuthorizationException : RewardServiceException()
}
