package org.mozilla.rocket.content.game.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import java.io.InputStream
import java.net.URL

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

    override suspend fun getBitmapFromImageLink(imageUrl: String): Result<Bitmap> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val inputStream = URL(imageUrl).content as InputStream
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                Result.Success(imageBitmap)
            },
            errorMessage = "Unable to get bitmap from the image link: $imageUrl"
        )
    }

    override suspend fun addRecentlyPlayedGame(game: ApiItem) {
        throw UnsupportedOperationException("Can't add recently played game to server")
    }

    override suspend fun removeRecentlyPlayedGame(game: ApiItem) {
        throw UnsupportedOperationException("Can't remove recently played game from server")
    }

    override suspend fun getRecentlyPlayedGameList(): Result<ApiEntity> {
        throw UnsupportedOperationException("Can't get recently played game from server")
    }

    override suspend fun getMyGameList(downloadGameList: ApiEntity): Result<ApiEntity> {
        throw UnsupportedOperationException("Can't get my game list from server")
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

    override fun shouldShowRecentPlayedSpotlight(): Boolean {
        TODO("not implemented")
    }

    override fun setRecentPlayedSpotlightHasShown() {
        TODO("not implemented")
    }

    companion object {
        private const val STR_INSTANT_GAME_ENDPOINT = "str_instant_game_endpoint"
        private const val STR_DOWNLOAD_GAME_ENDPOINT = "str_apk_game_endpoint"
        private const val DEFAULT_INSTANT_GAME_URL_ENDPOINT = "https://zerda-dcf76.appspot.com/api/v1/content?locale=all&category=html5Game"
        private const val DEFAULT_DOWNLOAD_GAME_URL_ENDPOINT = "https://zerda-dcf76.appspot.com/api/v1/content?locale=all&category=apkGame"
    }
}