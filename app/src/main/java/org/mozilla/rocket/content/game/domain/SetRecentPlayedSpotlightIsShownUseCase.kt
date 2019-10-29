package org.mozilla.rocket.content.game.domain

import org.mozilla.rocket.content.game.data.GameRepository

class SetRecentPlayedSpotlightIsShownUseCase(private val gameRepository: GameRepository) {
    operator fun invoke() {
        gameRepository.setRecentPlayedSpotlightHasShown()
    }
}