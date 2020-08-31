package org.mozilla.rocket.content.game.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_game_category.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.common.ui.StartSnapHelper
import org.mozilla.rocket.content.common.ui.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.game.ui.InstantGameViewModel
import org.mozilla.rocket.content.game.ui.model.Game
import org.mozilla.rocket.content.game.ui.model.GameCategory

class InstantGameCategoryAdapterDelegate(
    private val instantGameViewModel: InstantGameViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        InstantGameCategoryViewHolder(view, instantGameViewModel, telemetryViewModel)
}

class InstantGameCategoryViewHolder(
    override val containerView: View,
    instantGameViewModel: InstantGameViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(Game::class, R.layout.item_game, InstantGameAdapterDelegate(instantGameViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        game_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        game_list.adapter = this@InstantGameCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(game_list)
        game_list.monitorScrollImpression(telemetryViewModel)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val gameCategoryItem = uiModel as GameCategory
        category_title.text = if (gameCategoryItem.stringResourceId != 0) {
            category_title.context.getString(gameCategoryItem.stringResourceId)
        } else {
            gameCategoryItem.name
        }
        adapter.setData(gameCategoryItem.items)

        if (!gameCategoryItem.items.isNullOrEmpty() && gameCategoryItem.items[0] is Game) {
            game_list.firstImpression(
                telemetryViewModel,
                TelemetryWrapper.Extra_Value.INSTANT_GAME,
                (gameCategoryItem.items[0] as Game).subCategoryId
            )
        }
    }
}