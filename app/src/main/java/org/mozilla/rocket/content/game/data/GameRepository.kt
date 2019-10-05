package org.mozilla.rocket.content.game.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

class GameRepository(private val remoteDataSource: GameRemoteDataSource) {

    suspend fun getInstantGameList(): Result<ApiEntity> = remoteDataSource.getInstantGameList()

    suspend fun getDownloadGameList(): Result<ApiEntity> = remoteDataSource.getDownloadGameList()
}