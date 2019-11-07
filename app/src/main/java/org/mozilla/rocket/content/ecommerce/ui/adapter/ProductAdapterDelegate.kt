package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_product.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.DealViewModel
import kotlin.math.roundToInt

class ProductAdapterDelegate(private val dealViewModel: DealViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ProductViewHolder(view, dealViewModel)
}

class ProductViewHolder(
    override val containerView: View,
    private val dealViewModel: DealViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val productItem = uiModel as ProductItem
        product_name.text = productItem.title
        product_brand.text = productItem.source
        product_price.text = productItem.price
        product_rating.updateRatingInfo(productItem.ratingCount, productItem.reviews)
        product_discount.apply {
            if (productItem.discount.isNotEmpty()) {
                product_discount.text = productItem.discount
                product_discount.visibility = View.VISIBLE
            } else {
                product_discount.visibility = View.GONE
            }
        }

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .fitCenter()
            .load(productItem.imageUrl)
            .into(product_image)

        itemView.setOnClickListener { dealViewModel.onProductItemClicked(productItem) }
    }
}

data class ProductItem(
    val source: String,
    val category: String,
    val subCategoryId: String,
    val imageUrl: String,
    val linkUrl: String,
    val title: String,
    val componentId: String,
    val price: String = "",
    var discount: String = "",
    var rating: Float = 0F,
    var reviews: String = ""
) : DelegateAdapter.UiModel() {

    val ratingCount: Int
        get() = rating.roundToInt()
}