package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class CityAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CityViewHolder(view)
}

class CityViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val cityItem = uiModel as CityItem
        city_name.text = cityItem.title

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .fitCenter()
            .load(cityItem.imageUrl)
            .into(city_image)

        //TODO handle click with view model
        //itemView.setOnClickListener {}
    }
}

data class CityItem(
    val imageUrl: String,
    val linkUrl: String,
    val title: String
) : DelegateAdapter.UiModel()