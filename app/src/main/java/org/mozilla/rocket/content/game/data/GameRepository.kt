package org.mozilla.rocket.content.game.data

import android.graphics.Bitmap
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

class GameRepository(private val remoteDataSource: GameDataSource) {

    suspend fun getInstantGameList(): Result<ApiEntity> = remoteDataSource.getInstantGameList()

    suspend fun getDownloadGameList(): Result<ApiEntity> = remoteDataSource.getDownloadGameList()

    suspend fun getBitmapFromImageLink(imageUrl: String): Result<Bitmap> = remoteDataSource.getBitmapFromImageLink(imageUrl)
}