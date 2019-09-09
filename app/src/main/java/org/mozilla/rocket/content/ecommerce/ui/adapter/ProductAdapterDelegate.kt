package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_product.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ProductAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ProductViewHolder(view, shoppingViewModel)
}

class ProductViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val productItem = uiModel as ProductItem
        product_name.text = productItem.name
        product_brand.text = productItem.brand
        product_currency.text = productItem.currency
        product_price.text = productItem.priceWithFormat
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

        itemView.setOnClickListener { shoppingViewModel.onProductItemClicked(productItem) }
    }
}

data class ProductItem(
    val id: Int,
    val name: String,
    val currency: String,
    val price: Int,
    val discount: String,
    val brand: String,
    val linkUrl: String,
    val imageUrl: String,
    val rating: Float,
    val reviews: Int
) : DelegateAdapter.UiModel() {

    val priceWithFormat: String
        get() = DecimalFormat("#,###").format(price)

    val ratingCount: Int
        get() = rating.roundToInt()
}
