package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game.image
import kotlinx.android.synthetic.main.item_game.name
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.GamesViewModel

class GameAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            GameViewHolder(view, gamesViewModel)
}

class GameViewHolder(
    override val containerView: View,
    private val gamesViewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameItem = uiModel as GameItem
        name.text = gameItem.name
        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(gameItem.imageUrl)
                .into(image)

        itemView.setOnClickListener { gamesViewModel.onGameItemClicked(gameItem) }
    }
}

data class GameItem(val name: String, val imageUrl: String, val linkUrl: String) : DelegateAdapter.UiModel()