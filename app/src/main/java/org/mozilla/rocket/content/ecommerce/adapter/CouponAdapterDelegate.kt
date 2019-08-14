package org.mozilla.rocket.content.ecommerce.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_coupon.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ShoppingViewModel
import org.mozilla.rocket.content.ecommerce.data.ShoppingLink
import java.text.SimpleDateFormat
import java.util.*

class CouponAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CouponViewHolder(view, shoppingViewModel)
}

class CouponViewHolder(
        override val containerView: View,
        private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val couponItem = uiModel as Coupon

        coupon_item.setOnClickListener { shoppingViewModel.onCouponItemClicked(it.context, couponItem) }

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .fitCenter()
            .load(couponItem.link.image)
            .into(coupon_item_image)

        coupon_item_headline.text = couponItem.link.name

        val validPeriodStr = toValidPeriodFormat(couponItem.end)
        if (validPeriodStr.isNotEmpty()) {
            coupon_item_time.text = validPeriodStr
            coupon_item_time.visibility = View.VISIBLE
        } else {
            coupon_item_time.visibility = View.GONE
        }
    }

    private fun toValidPeriodFormat(end: Long): String
            = if (end != 0L) containerView.context.getString(R.string.coupon_valid_description, dateFormat.format(end)) else ""
}

object CouponKey {
    const val KEY_ID = "id"
    const val KEY_CATEGORY = "category"
    const val KEY_SUBCATEGORY = "subcategory"
    const val KEY_FEED = "feed"
    const val KEY_START = "start"
    const val KEY_END = "end"
    const val KEY_ACTIVE = "active"
}

data class Coupon(
    val id: String,
    val category: String,
    val subcategory: String,
    val feed: String,
    val start: Long,
    val end: Long,
    val active: Boolean,
    val link: ShoppingLink
) : DelegateAdapter.UiModel()
