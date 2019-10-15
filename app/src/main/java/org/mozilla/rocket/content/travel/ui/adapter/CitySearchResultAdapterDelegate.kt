package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city_search_result.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelCitySearchViewModel

class CitySearchResultAdapterDelegate(private val searchViewModel: TravelCitySearchViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder = CitySearchResultViewHolder(view, searchViewModel)
}

class CitySearchResultViewHolder(override val containerView: View, private val searchViewModel: TravelCitySearchViewModel) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as CitySearchResultUiModel
        title.text = uiModel.name
        containerView.setOnClickListener {
            searchViewModel.onCityClicked(uiModel)
        }
    }
}

data class CitySearchResultUiModel(
    val id: Int,
    val name: CharSequence
) : DelegateAdapter.UiModel()
