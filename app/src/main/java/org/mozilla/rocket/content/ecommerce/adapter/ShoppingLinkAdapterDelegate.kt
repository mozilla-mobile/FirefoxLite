package org.mozilla.rocket.content.ecommerce.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_shoppinglink.*
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ShoppingViewModel

class ShoppingLinkAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            ShoppingLinkViewHolder(view, shoppingViewModel)
}

class ShoppingLinkViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val shoppingLinkItem = uiModel as ShoppingLink

        shoppinglink_item.setOnClickListener {
            shoppingViewModel.onShoppingLinkItemClicked(it.context, shoppingLinkItem)
        }

        shoppinglink_category_text.text = shoppingLinkItem.name

        DrawableUtils.getAndroidDrawable(itemView.context, shoppingLinkItem.image)?.let {
            shoppinglink_category_image.setImageDrawable(it)
        }
    }
}

object ShoppingLinkKey {
    const val KEY_NAME = "name"
    const val KEY_URL = "url"
    const val KEY_IMAGE = "img"
    const val KEY_SOURCE = "source"
}

data class ShoppingLink(
    val url: String,
    val name: String,
    val image: String,
    val source: String
) : DelegateAdapter.UiModel()