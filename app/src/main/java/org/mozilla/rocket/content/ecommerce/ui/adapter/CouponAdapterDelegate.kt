package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.item_coupon.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel
import java.util.Locale

class CouponAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CouponViewHolder(view, shoppingViewModel)
}

class CouponViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    private val shapeMap: HashMap<String, Int> = hashMapOf(
            "tokopedia" to R.color.colorCouponTokopedia,
            "jd.id" to R.color.colorCouponJDID,
            "shopee" to R.color.colorCouponShopee,
            "lazada" to R.color.colorCouponLazada,
            "bukalapak" to R.color.colorCouponBukalapak,
            "flipkart" to R.color.colorCouponFlipkart,
            "amazon" to R.color.colorCouponAmazon,
            "snapdeal" to R.color.colorCouponSnapdeal,
            "paytm" to R.color.colorCouponPaytm,
            "shopclues" to R.color.colorCouponShopclues
    )

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val couponItem = uiModel as Coupon

        itemView.setOnClickListener { shoppingViewModel.onCouponItemClicked(couponItem) }

        coupon_brand.text = couponItem.brand
        coupon_title.text = couponItem.title
        coupon_description.text = couponItem.description
        coupon_remain.text = "${couponItem.remain} days left"

        val couponShape = DrawableUtils.loadAndTintDrawable(itemView.context, R.drawable.bg_coupon_shape,
                ContextCompat.getColor(itemView.context, shapeMap[couponItem.brand.toLowerCase(Locale.getDefault())] ?: R.color.colorCouponDefault))
        coupon_shape.setImageDrawable(couponShape)
    }
}

data class Coupon(
    val id: Int,
    val description: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val remain: Int,
    val linkUrl: String,
    val brand: String
) : DelegateAdapter.UiModel()
