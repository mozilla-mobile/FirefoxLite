package org.mozilla.rocket.content.games.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

interface GameDataSource {
    suspend fun getInstantGameList(): Result<ApiEntity>
    suspend fun getDownloadGameList(): Result<ApiEntity>
}