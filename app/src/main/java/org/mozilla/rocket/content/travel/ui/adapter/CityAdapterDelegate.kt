package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel

class CityAdapterDelegate(private val travelExploreViewModel: TravelExploreViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CityViewHolder(view, travelExploreViewModel)
}

class CityViewHolder(
    override val containerView: View,
    private val travelExploreViewModel: TravelExploreViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val cityItem = uiModel as CityUiModel
        city_name.text = cityItem.name

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .fitCenter()
            .load(cityItem.imageUrl)
            .into(city_image)

        itemView.setOnClickListener { travelExploreViewModel.onCityItemClicked(cityItem) }
    }
}

data class CityUiModel(
    val id: String,
    val imageUrl: String,
    val name: String
) : DelegateAdapter.UiModel()