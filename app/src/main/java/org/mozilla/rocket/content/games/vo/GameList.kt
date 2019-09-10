package org.mozilla.rocket.content.games.vo

import org.mozilla.rocket.adapter.DelegateAdapter

data class GameList(
    val type: String,
    val data: List<Game>
) : DelegateAdapter.UiModel()