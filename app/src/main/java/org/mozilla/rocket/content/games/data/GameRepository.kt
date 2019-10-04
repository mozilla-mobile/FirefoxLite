package org.mozilla.rocket.content.games.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

class GameRepository(private val localDataSource: GameLocalDataSource) {

    suspend fun getInstantGameList(): Result<ApiEntity> = localDataSource.getInstantGameList()

    suspend fun getDownloadGameList(): Result<ApiEntity> = localDataSource.getDownloadGameList()
}