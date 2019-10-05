package org.mozilla.rocket.content.games.ui.model

import org.mozilla.rocket.adapter.DelegateAdapter

data class GameCategory(
    val type: String,
    val name: String,
    val items: List<DelegateAdapter.UiModel>
) : DelegateAdapter.UiModel()