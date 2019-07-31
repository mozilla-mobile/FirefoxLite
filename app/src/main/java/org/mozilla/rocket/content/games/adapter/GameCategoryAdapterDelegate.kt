package org.mozilla.rocket.content.games.adapter

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter

class GameCategoryAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            GameCategoryViewHolder(view)
}

class GameCategoryViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(GameItem::class, R.layout.item_game, GameAdapterDelegate())
        }
    )

    init {
        game_list.apply {
            adapter = this@GameCategoryViewHolder.adapter
            layoutManager = LinearLayoutManager(containerView.context, RecyclerView.HORIZONTAL, false)
        }
    }

    override fun bind(uiModel: DelegateAdapter.UIModel) {
        uiModel as GameCategory
        category_title.text = uiModel.title
        adapter.setData(uiModel.gameList)
    }
}

data class GameCategory(val title: String, val gameList: List<GameItem>) : DelegateAdapter.UIModel()