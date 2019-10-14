package org.mozilla.rocket.content.game.ui.adapter

import android.view.ContextMenu
import android.view.View
import kotlinx.android.synthetic.main.item_game.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.game.ui.DownloadGameViewModel
import org.mozilla.rocket.content.game.ui.model.Game

class DownloadGameAdapterDelegate(private val downloadGameViewModel: DownloadGameViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        GameViewHolder(view, downloadGameViewModel)
}

class GameViewHolder(
    override val containerView: View,
    private val downloadGameViewModel: DownloadGameViewModel
) : DelegateAdapter.ViewHolder(containerView), View.OnCreateContextMenuListener {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameItem = uiModel as Game
        game_name.text = gameItem.name
        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .load(gameItem.imageUrl)
            .into(game_image)

        itemView.setOnClickListener { downloadGameViewModel.onGameItemClicked(gameItem) }
        itemView.setOnLongClickListener { downloadGameViewModel.onGameItemLongClicked(gameItem) }
        itemView.setOnCreateContextMenuListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.let {
            downloadGameViewModel.onCreateContextMenu(it)
        }
    }
}