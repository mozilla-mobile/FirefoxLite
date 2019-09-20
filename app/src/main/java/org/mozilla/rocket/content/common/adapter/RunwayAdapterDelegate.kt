package org.mozilla.rocket.content.common.adapter

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import kotlinx.android.synthetic.main.item_runway_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration

class RunwayAdapterDelegate(private val runwayViewModel: RunwayViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RunwayViewHolder(view, runwayViewModel)
}

class RunwayViewHolder(
    override val containerView: View,
    private val runwayViewModel: RunwayViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(RunwayItem::class, R.layout.item_runway, RunwayItemAdapterDelegate(runwayViewModel))
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