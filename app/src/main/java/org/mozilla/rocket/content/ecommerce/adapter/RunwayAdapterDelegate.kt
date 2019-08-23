package org.mozilla.rocket.content.ecommerce.adapter

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import kotlinx.android.synthetic.main.item_runway_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.ecommerce.ShoppingViewModel

class RunwayAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RunwayViewHolder(view, shoppingViewModel)
}

class RunwayViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(RunwayItem::class, R.layout.item_runway, RunwayItemAdapterDelegate(shoppingViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        val padding = itemView.resources.getDimensionPixelSize(R.dimen.card_padding)
        runway_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth, padding))
        runway_list.adapter = this@RunwayViewHolder.adapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(runway_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val runway = uiModel as Runway
        adapter.setData(runway.runwayItems)
    }
}

data class Runway(val runwayItems: List<RunwayItem>) : DelegateAdapter.UiModel()