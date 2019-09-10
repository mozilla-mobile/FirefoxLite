package org.mozilla.rocket.content.games.vo

import org.mozilla.rocket.adapter.DelegateAdapter
import java.util.UUID

data class Game(
    val id: UUID,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val type: String,
    val recentplay: Boolean = false,
    val installed: Boolean = false
) : DelegateAdapter.UiModel()