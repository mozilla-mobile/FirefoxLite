package org.mozilla.rocket.content.games.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game.image
import kotlinx.android.synthetic.main.item_game.name
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class GameAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            GameViewHolder(view)
}

class GameViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UIModel) {
        val gameItem = uiModel as GameItem
        name.text = gameItem.name
        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(gameItem.imageUrl)
                .into(image)

        itemView.setOnClickListener {
            // TODO
        }
    }
}

data class GameItem(val name: String, val imageUrl: String) : DelegateAdapter.UIModel()