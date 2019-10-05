package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game_category.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.games.ui.DownloadGameViewModel
import org.mozilla.rocket.content.games.ui.model.Game
import org.mozilla.rocket.content.games.ui.model.GameCategory

class DownloadGameCategoryAdapterDelegate(private val downloadGameViewModel: DownloadGameViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        DownloadGameCategoryViewHolder(view, downloadGameViewModel)
}

class DownloadGameCategoryViewHolder(
    override val containerView: View,
    downloadGameViewModel: DownloadGameViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(Game::class, R.layout.item_game, DownloadGameAdapterDelegate(downloadGameViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        game_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        game_list.adapter = this@DownloadGameCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(game_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameCategoryItem = uiModel as GameCategory
        category_title.text = gameCategoryItem.name
        adapter.setData(gameCategoryItem.items)
    }
}
