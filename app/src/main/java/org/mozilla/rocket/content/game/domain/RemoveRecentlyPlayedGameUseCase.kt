package org.mozilla.rocket.content.game.domain

import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.game.data.GameRepository

class RemoveRecentlyPlayedGameUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(game: ApiItem) {
        return repository.removeRecentlyPlayedGame(game)
    }
}