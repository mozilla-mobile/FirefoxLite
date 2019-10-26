package org.mozilla.rocket.content.game.ui.model

import org.mozilla.rocket.adapter.DelegateAdapter

data class Game(
    val brand: String,
    val category: String,
    val subCategoryId: String,
    val imageUrl: String,
    val linkUrl: String,
    val name: String,
    val packageName: String,
    val componentId: String,
    val gameType: GameType
) : DelegateAdapter.UiModel()

sealed class GameType {
    object Normal : GameType()
    object RecentlyPlayed : GameType()
    object MyGame : GameType()
}