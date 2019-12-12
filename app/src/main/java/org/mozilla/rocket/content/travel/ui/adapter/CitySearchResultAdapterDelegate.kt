package org.mozilla.rocket.content.travel.ui.adapter

import android.text.TextUtils
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
        if (uiModel.country.isEmpty()) {
            title.text = uiModel.name
        } else {
            title.text = TextUtils.concat(uiModel.name, ", ", uiModel.country)
        }
        containerView.setOnClickListener {
            searchViewModel.onCityClicked(uiModel)
        }
    }
}

data class CitySearchResultUiModel(
    val id: String,
    val name: CharSequence,
    val country: String,
    val countryCode: String,
    val type: String
) : DelegateAdapter.UiModel()
