package org.mozilla.rocket.content.game.domain

import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.game.data.GameRepository

class AddRecentlyPlayedGameUseCase(private val repository: GameRepository) {

    suspend operator fun invoke(game: ApiItem) {
        return repository.addRecentlyPlayedGame(game)
    }
}