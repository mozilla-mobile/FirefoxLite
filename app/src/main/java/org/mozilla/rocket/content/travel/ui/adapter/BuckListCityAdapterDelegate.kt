package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_bucket_list.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelBucketListViewModel

class BucketListCityAdapterDelegate(private val travelBucketListViewModel: TravelBucketListViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        BucketListCityViewHolder(view, travelBucketListViewModel)
}

class BucketListCityViewHolder(
    override val containerView: View,
    private val travelBucketListViewModel: TravelBucketListViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val bucketListCity = uiModel as BucketListCityUiModel
        city_name.text = bucketListCity.name

        val placeholderArray = itemView.resources.obtainTypedArray(R.array.travel_placeholders)
        val placeholder = placeholderArray.getResourceId((0 until placeholderArray.length()).random(), R.drawable.travel_card1)
        placeholderArray.recycle()

        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(placeholder)
                .fitCenter()
                .load(bucketListCity.imageUrl)
                .into(city_image)

        itemView.setOnClickListener { travelBucketListViewModel.onBucketListCityClicked(bucketListCity) }
    }
}

data class BucketListCityUiModel(
    val id: String,
    val imageUrl: String,
    val name: String,
    val type: String,
    val nameInEnglish: String,
    val countryCode: String
) : DelegateAdapter.UiModel() {
    fun getTelemetryItemName() = String.format("%s-%s", countryCode, nameInEnglish)
}