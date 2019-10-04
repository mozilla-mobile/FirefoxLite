package org.mozilla.rocket.content.games.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.AssetsUtils

class GameLocalDataSource(private val appContext: Context) : GameDataSource {

    override suspend fun getInstantGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            ApiEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.game_mock_items))
        )
    }

    override suspend fun getDownloadGameList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            ApiEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.game_mock_items))
        )
    }
}