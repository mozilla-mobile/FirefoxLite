package org.mozilla.rocket.content.games.ui.model

import org.mozilla.rocket.adapter.DelegateAdapter

data class Game(
    val brand: String,
    val imageUrl: String,
    val linkUrl: String,
    val name: String,
    val packageName: String,
    val componentId: String
) : DelegateAdapter.UiModel()