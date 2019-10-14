package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_section_header.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.data.SectionType
import org.mozilla.rocket.content.travel.data.SectionType.Explore
import org.mozilla.rocket.content.travel.data.SectionType.TopHotels
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel

class SectionHeaderAdapterDelegate(private val travelCityViewModel: TravelCityViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        SectionHeaderViewHolder(view, travelCityViewModel)
}

class SectionHeaderViewHolder(
    override val containerView: View,
    private val travelCityViewModel: TravelCityViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val header = uiModel as SectionHeaderUiModel
        when (header.type) {
            is Explore -> {
                header_title.text = containerView.context.getString(R.string.travel_detail_section_explore_title, header.type.name)
                header_more.isVisible = false
            }
            is TopHotels -> {
                header_title.text = containerView.context.getString(R.string.travel_detail_section_hotel_title)
                header_more.isVisible = true
            }
        }
    }
}

data class SectionHeaderUiModel(
    val type: SectionType
) : DelegateAdapter.UiModel()
