package org.mozilla.rocket.content.common.adapter

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import kotlinx.android.synthetic.main.item_runway_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.common.ui.HorizontalSpaceItemDecoration

class RunwayAdapterDelegate(
    private val runwayViewModel: RunwayViewModel,
    private val category: String = "",
    private val telemetryViewModel: VerticalTelemetryViewModel? = null
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        RunwayViewHolder(view, runwayViewModel, category, telemetryViewModel)
}

class RunwayViewHolder(
    override val containerView: View,
    private val runwayViewModel: RunwayViewModel,
    private val category: String = "",
    private val telemetryViewModel: VerticalTelemetryViewModel? = null
) : DelegateAdapter.ViewHolder(containerView) {

    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(RunwayItem::class, R.layout.item_runway, RunwayItemAdapterDelegate(runwayViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        runway_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        runway_list.adapter = this@RunwayViewHolder.adapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(runway_list)
        telemetryViewModel?.let {
            runway_list.monitorScrollImpression(it)
        }
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val runway = uiModel as Runway
        adapter.setData(runway.items)

        if (category.isNotEmpty() && telemetryViewModel != null && !runway.items.isNullOrEmpty()) {
            runway_list.firstImpression(
                telemetryViewModel,
                category,
                runway.items[0].subCategoryId
            )
        }
    }
}

data class Runway(
    val componentType: String,
    val subcategoryName: String,
    val subcategoryId: Int,
    val items: List<RunwayItem>
) : DelegateAdapter.UiModel()