package org.mozilla.rocket.content.games.ui.adapter

import android.content.Intent
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.GamesViewModel
import android.view.ContextMenu
import kotlinx.android.synthetic.main.item_game.*

class GameAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        GameViewHolder(view, gamesViewModel)
}

class GameViewHolder(
    override val containerView: View,
    private val gamesViewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView), View.OnCreateContextMenuListener {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameItem = uiModel as Game
        game_name.text = gameItem.name
        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .load(gameItem.imageUrl)
            .into(game_image)

        itemView.setOnClickListener { gamesViewModel.onGameItemClicked(gameItem) }
        itemView.setOnLongClickListener { gamesViewModel.onGameItemLongClicked(gameItem) }
        itemView.setOnCreateContextMenuListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.setHeaderTitle(game_name.text)
        val intent = Intent()
        intent.putExtra("gameType", gamesViewModel.selectedGame.type)
//        if (gamesViewModel.canShare())
//            menu?.add(0, R.id.share, 0, R.string.gaming_vertical_menu_option_1)
//        if (gamesViewModel.canCreateShortCut())
//            menu?.add(0, R.id.shortcut, 0, R.string.gaming_vertical_menu_option_2)
//        if (gamesViewModel.canRemoveFromList())
//            menu?.add(0, R.id.remove, 0, R.string.gaming_vertical_menu_option_3)
    }
}

data class Game(
    val brand: String,
    val imageUrl: String,
    val linkUrl: String,
    val name: String,
    val packageName: String,
    val componentId: String,
    val type: GameType = GameType.INSTANT
) : DelegateAdapter.UiModel()

enum class GameType {
    INSTANT, DOWNLOAD
}