package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.games.ui.GamesViewModel

class GameCategoryAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        GameCategoryViewHolder(view, gamesViewModel)
}

class GameCategoryViewHolder(
    override val containerView: View,
    gameViewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(Game::class, R.layout.item_game, GameAdapterDelegate(gameViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        game_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        game_list.adapter = this@GameCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(game_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameCategoryItem = uiModel as GameCategory
        category_title.text = gameCategoryItem.name
        adapter.setData(gameCategoryItem.items)
    }
}

data class GameCategory(
    val type: String,
    val name: String,
    val items: List<DelegateAdapter.UiModel>
) : DelegateAdapter.UiModel()