package org.mozilla.rocket.content.games.vo

import org.mozilla.rocket.adapter.DelegateAdapter

data class Game(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val packageName: String = "",
    val type: String = "",
    val recentplay: Boolean = false,
    val installed: Boolean = false
) : DelegateAdapter.UiModel()