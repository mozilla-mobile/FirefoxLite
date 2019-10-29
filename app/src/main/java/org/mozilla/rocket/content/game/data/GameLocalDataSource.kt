package org.mozilla.rocket.content.game.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiCategory
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.strictmodeviolator.StrictModeViolation

class GameLocalDataSource(private val appContext: Context) : GameDataSource {

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
        saveRecentlyPlayedGameList(game, game)
    }

    override suspend fun removeRecentlyPlayedGame(game: ApiItem) = withContext(Dispatchers.IO) {
        saveRecentlyPlayedGameList(null, game)
    }

    override suspend fun getRecentlyPlayedGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        val jsonString = preference.getString(KEY_RECENTLY_PLAYED, "")
        return@withContext if (jsonString?.isNotEmpty() == true) {
            Result.Success(ApiEntity.fromJson(jsonString))
        } else {
            Result.Error(IllegalArgumentException("No recently played game saved"))
        }
    }

    override suspend fun getMyGameList(downloadGameList: ApiEntity): Result<ApiEntity> = withContext(Dispatchers.IO) {
        val myGameList = ArrayList<ApiItem>()

        for (subcategory in downloadGameList.subcategories) {
            for (item in subcategory.items) {
                if (isPackageInstalled(item.description)) {
                    myGameList.add(item)
                }
            }
        }

        val apiCategory = ApiCategory(MY_GAME, appContext.getString(R.string.gaming_vertical_genre_3), -1, myGameList)
        val apiEntity = ApiEntity(1, listOf(apiCategory))

        return@withContext Result.Success(apiEntity)
    }

    private suspend fun saveRecentlyPlayedGameList(gameToBeAdded: ApiItem?, gameToBeFiltered: ApiItem) {
        val result = getRecentlyPlayedGameList()
        val recentlyPlayedList = ArrayList<ApiItem>()

        gameToBeAdded?.let { recentlyPlayedList.add(it) }

        if (result is Result.Success && result.data.subcategories.isNotEmpty()) {
            recentlyPlayedList.addAll(
                result.data.subcategories[0].items.filter {
                    it.destination != gameToBeFiltered.destination
                }
            )
        }

        val apiCategory = ApiCategory(RECENT, RECENTLY_PLAYED_SUB_CATEGORY_NAME, RECENTLY_PLAYED_SUB_CATEGORY_ID, recentlyPlayedList)
        val apiEntity = ApiEntity(1, listOf(apiCategory))

        preference.edit().putString(KEY_RECENTLY_PLAYED, apiEntity.toJsonObject().toString()).apply()
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        var hasInstalled = true
        try {
            appContext.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            hasInstalled = false
        }
        return hasInstalled
    }

    override fun shouldShowRecentPlayedSpotlight(): Boolean {
        val jsonString = preference.getString(KEY_RECENTLY_PLAYED, "")
        return preference.getBoolean(KEY_SPOTLIGHT, true) && !jsonString.isNullOrEmpty()
    }

    override fun setRecentPlayedSpotlightHasShown() {
        preference.edit().putBoolean(KEY_SPOTLIGHT, false).apply()
    }

    companion object {
        private const val PREF_NAME = "game"
        private const val KEY_RECENTLY_PLAYED = "recently_played"
        private const val KEY_SPOTLIGHT = "recently_played_spotlight"
        const val RECENTLY_PLAYED_SUB_CATEGORY_NAME = "Recently Played"
        const val RECENTLY_PLAYED_SUB_CATEGORY_ID = 24
    }
}