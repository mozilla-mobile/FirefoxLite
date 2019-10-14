package org.mozilla.rocket.content.game.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiCategory
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.strictmodeviolator.StrictModeViolation

class GameLocalDataSource(appContext: Context) : GameDataSource {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    override suspend fun getInstantGameList(): Result<ApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getDownloadGameList(): Result<ApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getBitmapFromImageLink(imageUrl: String): Result<Bitmap> {
        TODO("not implemented")
    }

    override suspend fun addRecentlyPlayedGame(game: ApiItem) = withContext(Dispatchers.IO) {
        val result = getRecentlyPlayedGameList()
        val recentlyPlayedList = arrayListOf(game)

        if (result is Result.Success && result.data.subcategories.isNotEmpty()) {
            recentlyPlayedList.addAll(
                // remove duplicated item
                result.data.subcategories[0].items.filter {
                    it.destination != game.destination
                }
            )
        }

        val apiCategory = ApiCategory("Scard", "Recently played", -1, recentlyPlayedList)
        val apiEntity = ApiEntity(1, listOf(apiCategory))
        preference.edit().putString(KEY_RECENTLY_PLAYED, apiEntity.toJsonObject().toString()).apply()
    }

    override suspend fun getRecentlyPlayedGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        val jsonString = preference.getString(KEY_RECENTLY_PLAYED, "")
        return@withContext if (jsonString?.isNotEmpty() == true) {
            Result.Success(ApiEntity.fromJson(jsonString))
        } else {
            Result.Error(IllegalArgumentException("No recently played game saved"))
        }
    }

    companion object {
        private const val PREF_NAME = "game"
        private const val KEY_RECENTLY_PLAYED = "recently_played"
    }
}