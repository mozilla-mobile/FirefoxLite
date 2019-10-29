package org.mozilla.rocket.content.game.data

import android.graphics.Bitmap
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem

class GameRepository(
    private val remoteDataSource: GameDataSource,
    private val localDataSource: GameDataSource
) {

    suspend fun getInstantGameList(): Result<ApiEntity> = remoteDataSource.getInstantGameList()

    suspend fun getDownloadGameList(): Result<ApiEntity> = remoteDataSource.getDownloadGameList()

    suspend fun getBitmapFromImageLink(imageUrl: String): Result<Bitmap> = remoteDataSource.getBitmapFromImageLink(imageUrl)

    suspend fun addRecentlyPlayedGame(game: ApiItem) = localDataSource.addRecentlyPlayedGame(game)

    suspend fun removeRecentlyPlayedGame(game: ApiItem) = localDataSource.removeRecentlyPlayedGame(game)

    suspend fun getRecentlyPlayedGames(): Result<ApiEntity> = localDataSource.getRecentlyPlayedGameList()

    suspend fun getMyGames(): Result<ApiEntity> {
        val result = remoteDataSource.getDownloadGameList()
        return if (result is Result.Success) {
            localDataSource.getMyGameList(result.data)
        } else {
            result
        }
    }

    fun shouldShowRecentPlayedSpotlight(): Boolean = localDataSource.shouldShowRecentPlayedSpotlight()

    fun setRecentPlayedSpotlightHasShown() = localDataSource.setRecentPlayedSpotlightHasShown()
}