package org.mozilla.rocket.content.games.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.games.data.GameRepository

class GetInstantGameListUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(): Result<ApiEntity> {
        return repository.getInstantGameList()
    }
}