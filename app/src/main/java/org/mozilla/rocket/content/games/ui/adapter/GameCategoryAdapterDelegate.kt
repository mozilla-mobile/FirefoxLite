package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.GameItemDecoration
import org.mozilla.rocket.content.games.ui.GamesViewModel

class GameCategoryAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        GameCategoryViewHolder(view, gamesViewModel)
}

class GameCategoryViewHolder(
    override val containerView: View,
    viewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(GameItem::class, R.layout.item_game, GameAdapterDelegate(viewModel))
        }
    )
    /* paddingLeftRight is padding for boundary and the first/last item
       paddingInBetween is padding for each item */
    private val paddingLeftRight = 16
    private val paddingInBetween = 4

    init {
        game_list.apply {
            adapter = this@GameCategoryViewHolder.adapter
            layoutManager = LinearLayoutManager(containerView.context, RecyclerView.HORIZONTAL, false)
        }
        game_list.addItemDecoration(GameItemDecoration(paddingLeftRight, paddingInBetween))
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as GameCategory
        category_title.text = uiModel.title
        adapter.setData(uiModel.gameList)
    }
}

data class GameCategory(
    val id: String,
    val title: String,
    val gameList: List<GameItem>
) : DelegateAdapter.UiModel()