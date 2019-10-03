package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_bucket_list.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class BucketListCityAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        BucketListCityViewHolder(view)
}

class BucketListCityViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val bucketListCity = uiModel as BucketListCityItem
        city_name.text = bucketListCity.cityName

        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(bucketListCity.imageUrl)
                .into(city_image)

        city_favorite_btn.isSelected = bucketListCity.isFavorite
    }
}

data class BucketListCityItem(
    val cityName: String,
    val imageUrl: String,
    val linkUrl: String,
    val isFavorite: Boolean
) : DelegateAdapter.UiModel()