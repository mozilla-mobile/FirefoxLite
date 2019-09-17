package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.item_runway.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel
import kotlin.math.min

class RunwayItemAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RunwayItemViewHolder(view, shoppingViewModel)
}

class RunwayItemViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    init {
        if (runway_card.layoutParams.width == FrameLayout.LayoutParams.MATCH_PARENT) {
            val padding = itemView.resources.getDimensionPixelSize(R.dimen.card_padding) * 2
            val displayMetrics = itemView.context.resources.displayMetrics
            val widthPixels = min(displayMetrics.widthPixels, displayMetrics.heightPixels)
            val planWidth = widthPixels - padding
            val planHeight = (planWidth * 0.5).toInt()
            runway_card.layoutParams = FrameLayout.LayoutParams(planWidth, planHeight)
        }
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val runwayItem = uiModel as RunwayItem

        runway_card.setOnClickListener { shoppingViewModel.onRunwayItemClicked(runwayItem) }

        runway_source.text = runwayItem.source

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.news_item_img_bg)
            .fitCenter()
            .load(runwayItem.imageUrl)
            .into(runway_image)
    }
}

data class RunwayItem(
    val id: Int,
    val imageUrl: String,
    val linkUrl: String,
    val source: String
) : DelegateAdapter.UiModel()