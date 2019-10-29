package org.mozilla.rocket.content.game.ui.model

import org.mozilla.rocket.adapter.DelegateAdapter

data class GameCategory(
    val type: String,
    val name: String,
    val stringResourceId: Int,
    val items: List<DelegateAdapter.UiModel>
) : DelegateAdapter.UiModel()