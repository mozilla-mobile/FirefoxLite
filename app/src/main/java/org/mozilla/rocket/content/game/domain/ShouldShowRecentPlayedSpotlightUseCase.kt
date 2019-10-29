package org.mozilla.rocket.content.game.domain

import org.mozilla.rocket.content.game.data.GameRepository

class ShouldShowRecentPlayedSpotlightUseCase(
    private val gameRepository: GameRepository
) {
    operator fun invoke(): Boolean {
        return gameRepository.shouldShowRecentPlayedSpotlight()
    }
}