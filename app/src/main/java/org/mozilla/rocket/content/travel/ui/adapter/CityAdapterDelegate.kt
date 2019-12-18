package org.mozilla.rocket.content.travel.ui.adapter

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import kotlinx.android.synthetic.main.item_city.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel
import java.util.Locale

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

        val placeholderArray = itemView.resources.obtainTypedArray(R.array.travel_placeholders)
        val placeholder = placeholderArray.getResourceId((0 until placeholderArray.length()).random(), R.drawable.travel_card1)
        placeholderArray.recycle()

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.travel_explore_item_radius)

        city_image.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.setRoundRect(0, 0, view!!.width, view.height, radius.toFloat())
            }
        }
        city_image.clipToOutline = true

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(placeholder)
            .fitCenter()
            .load(cityItem.imageUrl)
            .into(city_image)

        itemView.setOnClickListener { travelExploreViewModel.onCityItemClicked(cityItem) }
    }
}

data class CityUiModel(
    val id: String,
    val imageUrl: String,
    val name: String,
    val type: String,
    val nameInEnglish: String,
    val countryCode: String
) : DelegateAdapter.UiModel() {
    fun getTelemetryItemName() = String.format("%s-%s", countryCode, nameInEnglish.toLowerCase(Locale.getDefault()))
}