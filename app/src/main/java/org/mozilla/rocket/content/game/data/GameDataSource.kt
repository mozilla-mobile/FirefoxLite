package org.mozilla.rocket.content.game.data

import android.graphics.Bitmap
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem

const val BANNER = "banner"
const val RECENT = "_recent"

interface GameDataSource {
    suspend fun getInstantGameList(): Result<ApiEntity>
    suspend fun getDownloadGameList(): Result<ApiEntity>
    suspend fun getBitmapFromImageLink(imageUrl: String): Result<Bitmap>
    suspend fun addRecentlyPlayedGame(game: ApiItem)
    suspend fun removeRecentlyPlayedGame(game: ApiItem)
    suspend fun getRecentlyPlayedGameList(): Result<ApiEntity>
}