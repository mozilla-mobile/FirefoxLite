package org.mozilla.rocket.content.games.vo

import org.mozilla.rocket.adapter.DelegateAdapter

data class GameCategory(
    val type: String,
    val games: List<Game>
) : DelegateAdapter.UiModel()