package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.city_search.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class CitySearchAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CitySearchViewHolder(view)
}

class CitySearchViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val citySearch = uiModel as CitySearch

        city_search_edit_area.setOnClickListener {
            //TODO go search activity
        }

        city_search_title.text = citySearch.title
        city_search_hint.text = citySearch.hint
    }
}

data class CitySearch(
    val title: String,
    val hint: String
) : DelegateAdapter.UiModel()