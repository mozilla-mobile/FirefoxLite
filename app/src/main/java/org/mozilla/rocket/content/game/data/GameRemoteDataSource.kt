package org.mozilla.rocket.content.game.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.safeApiCall
import java.net.URL

class GameRemoteDataSource : GameDataSource {

    override suspend fun getInstantGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getInstantGameApiEndpoint())
                Result.Success(ApiEntity.fromJson(responseBody))
            },
            errorMessage = "Unable to get remote instant game list"
        )
    }

    override suspend fun getDownloadGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getDownloadGameApiEndpoint())
                Result.Success(ApiEntity.fromJson(responseBody))
            },
            errorMessage = "Unable to get remote download game list"
        )
    }

    private fun getInstantGameApiEndpoint(): String {
        val instantGameApiEndpoint = FirebaseHelper.getFirebase().getRcString(STR_INSTANT_GAME_ENDPOINT)
        return if (instantGameApiEndpoint.isNotEmpty()) {
            instantGameApiEndpoint
        } else {
            DEFAULT_INSTANT_GAME_URL_ENDPOINT
        }
    }

    private fun getDownloadGameApiEndpoint(): String {
        val instantGameApiEndpoint = FirebaseHelper.getFirebase().getRcString(STR_DOWNLOAD_GAME_ENDPOINT)
        return if (instantGameApiEndpoint.isNotEmpty()) {
            instantGameApiEndpoint
        } else {
            DEFAULT_DOWNLOAD_GAME_URL_ENDPOINT
        }
    }

    private fun getHttpResult(endpointUrl: String): String {
        var responseBody = HttpRequest.get(URL(endpointUrl), "")
        responseBody = responseBody.replace("\n", "")
        return responseBody
    }

    companion object {
        private const val STR_INSTANT_GAME_ENDPOINT = "str_instant_game_endpoint"
        private const val STR_DOWNLOAD_GAME_ENDPOINT = "str_apk_game_endpoint"
        private const val DEFAULT_INSTANT_GAME_URL_ENDPOINT = "https://rocket-dev01.appspot.com/api/v1/content?locale=all&category=html5Game"
        private const val DEFAULT_DOWNLOAD_GAME_URL_ENDPOINT = "https://rocket-dev01.appspot.com/api/v1/content?locale=all&category=apkGame"
    }
}