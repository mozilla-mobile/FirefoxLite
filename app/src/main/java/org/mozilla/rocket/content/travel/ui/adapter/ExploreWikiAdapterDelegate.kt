package org.mozilla.rocket.content.travel.ui.adapter

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import kotlinx.android.synthetic.main.item_travel_detail_wiki.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel

class ExploreWikiAdapterDelegate(private val travelCityViewModel: TravelCityViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ExploreWikiViewHolder(view, travelCityViewModel)
}

class ExploreWikiViewHolder(
    override val containerView: View,
    private val travelCityViewModel: TravelCityViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val exploreWiki = uiModel as WikiUiModel

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.travel_explore_item_radius)

        explore_wiki_image.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.setRoundRect(0, 0, view!!.width, view.height + radius, radius.toFloat())
            }
        }
        explore_wiki_image.clipToOutline = true

        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(exploreWiki.imageUrl)
                .into(explore_wiki_image)

        explore_wiki_content.text = exploreWiki.introduction
        explore_wiki_source.text = exploreWiki.sourceName

        itemView.setOnClickListener { travelCityViewModel.onWikiClicked(exploreWiki) }
    }
}

data class WikiUiModel(
    val imageUrl: String,
    val sourceName: String,
    val introduction: String,
    val linkUrl: String,
    val source: String = "wiki"
) : DelegateAdapter.UiModel()