package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game.image
import kotlinx.android.synthetic.main.item_game.name
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.GamesViewModel
import android.view.ContextMenu

class GameAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        GameViewHolder(view, gamesViewModel)
}

class GameViewHolder(
    override val containerView: View,
    private val gamesViewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView), View.OnCreateContextMenuListener {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        var gameItem = uiModel as GameItem
        name.text = gameItem.name
        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .fitCenter()
            .load(gameItem.imageUrl)
            .into(image)

        itemView.setOnClickListener { gamesViewModel.onGameItemClicked(gameItem) }
        itemView.setOnLongClickListener { gamesViewModel.onGameItemLongClicked(gameItem) }
        itemView.setOnCreateContextMenuListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.setHeaderTitle(name.text)
        if (gamesViewModel.canShare())
            menu?.add(0, R.id.share, 0, R.string.game_contextmenu_share)
        if (gamesViewModel.canCreateShortCut())
            menu?.add(0, R.id.shortcut, 0, R.string.game_contextmenu_create_shortcut)
        if (gamesViewModel.canRemoveFromList())
            menu?.add(0, R.id.remove, 0, R.string.game_contextmenu_remove_from_gamelist)
    }
}

data class GameItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val type: String,
    val recentplay: Boolean
) : DelegateAdapter.UiModel()
