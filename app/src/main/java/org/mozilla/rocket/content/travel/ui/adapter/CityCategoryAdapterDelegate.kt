package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city_category.*
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
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel

class CityCategoryAdapterDelegate(
    private val travelExploreViewModel: TravelExploreViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CityCategoryViewHolder(view, travelExploreViewModel, telemetryViewModel)
}

class CityCategoryViewHolder(
    override val containerView: View,
    private val travelExploreViewModel: TravelExploreViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(CityUiModel::class, R.layout.item_city, CityAdapterDelegate(travelExploreViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        city_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        city_list.adapter = this@CityCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(city_list)
        city_list.monitorScrollImpression(telemetryViewModel)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val cityCategory = uiModel as CityCategoryUiModel

        category_title.text = if (cityCategory.stringResourceId != 0) {
            category_title.context.getString(cityCategory.stringResourceId)
        } else {
            cityCategory.subcategoryName
        }

        adapter.setData(cityCategory.cityList)

        if (!cityCategory.cityList.isNullOrEmpty()) {
            city_list.firstImpression(
                telemetryViewModel,
                TelemetryWrapper.Extra_Value.EXPLORE,
                cityCategory.subcategoryId.toString()
            )
        }
    }
}

data class CityCategoryUiModel(
    val componentType: String,
    val subcategoryName: String,
    val stringResourceId: Int,
    val subcategoryId: Int,
    val cityList: List<CityUiModel>
) : DelegateAdapter.UiModel()