package org.mozilla.rocket.content.games.ui.adapter

import android.view.ContextMenu
import android.view.View
import kotlinx.android.synthetic.main.item_game.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.DownloadGameViewModel
import org.mozilla.rocket.content.games.ui.model.Game

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
        menu?.setHeaderTitle(game_name.text)
//        val intent = Intent()
//        intent.putExtra("gameType", downloadGameViewModel.selectedGame.type)
//        if (downloadGameViewModel.canShare())
//            menu?.add(0, R.id.share, 0, R.string.gaming_vertical_menu_option_1)
//        if (downloadGameViewModel.canCreateShortCut())
//            menu?.add(0, R.id.shortcut, 0, R.string.gaming_vertical_menu_option_2)
//        if (downloadGameViewModel.canRemoveFromList())
//            menu?.add(0, R.id.remove, 0, R.string.gaming_vertical_menu_option_3)
    }
}