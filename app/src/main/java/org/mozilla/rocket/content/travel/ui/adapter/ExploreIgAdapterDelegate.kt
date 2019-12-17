package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_travel_detail_ig.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel

class ExploreIgAdapterDelegate(private val travelCityViewModel: TravelCityViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ExploreIgViewHolder(view, travelCityViewModel)
}

class ExploreIgViewHolder(
    override val containerView: View,
    private val travelCityViewModel: TravelCityViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val exploreIg = uiModel as IgUiModel

        itemView.setOnClickListener {
            travelCityViewModel.onIgClicked(exploreIg)
        }

        explore_ig_tag.text = itemView.resources.getString(R.string.travel_content_ig_link, exploreIg.title)

        val igIconUri = TopSitesUtils.TOP_SITE_ASSET_PREFIX + ICON_FILE_NAME
        val resource = FavIconUtils.getBitmapFromUri(itemView.context, igIconUri)
        explore_ig_icon.setImageBitmap(resource)
    }

    companion object {
        internal const val ICON_FILE_NAME = "ic_instagram.png"
    }
}

data class IgUiModel(
    val title: String,
    val linkUrl: String,
    val source: String = "ig"
) : DelegateAdapter.UiModel()