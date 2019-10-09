package org.mozilla.rocket.content.game.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest

class GameRemoteDataSource : GameDataSource {

    override suspend fun getInstantGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(request = Request(url = getInstantGameApiEndpoint(), method = Request.Method.GET),
                    onSuccess = {
                        Result.Success(ApiEntity.fromJson(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get remote instant game list"
        )
    }

    override suspend fun getDownloadGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(request = Request(url = getDownloadGameApiEndpoint(), method = Request.Method.GET),
                    onSuccess = {
                        Result.Success(ApiEntity.fromJson(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
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

    companion object {
        private const val STR_INSTANT_GAME_ENDPOINT = "str_instant_game_endpoint"
        private const val STR_DOWNLOAD_GAME_ENDPOINT = "str_apk_game_endpoint"
        private const val DEFAULT_INSTANT_GAME_URL_ENDPOINT = "https://rocket-dev01.appspot.com/api/v1/content?locale=all&category=html5Game"
        private const val DEFAULT_DOWNLOAD_GAME_URL_ENDPOINT = "https://rocket-dev01.appspot.com/api/v1/content?locale=all&category=apkGame"
    }
}