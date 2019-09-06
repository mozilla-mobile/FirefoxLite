package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.View
import androidx.core.view.ViewCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.item_coupon.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel

class CouponAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CouponViewHolder(view, shoppingViewModel)
}

class CouponViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val couponItem = uiModel as Coupon

        itemView.setOnClickListener { shoppingViewModel.onCouponItemClicked(couponItem) }

        coupon_brand.text = couponItem.brand
        coupon_title.text = couponItem.title
        val remainFormat = itemView.context.getString(R.string.coupon_remain)
        coupon_remain.text = String.format(remainFormat, couponItem.remain)

        GlideApp.with(itemView.context)
            .asBitmap()
            .load(couponItem.imageUrl)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                    coupon_image.setImageBitmap(resource)
                    obtainBackgroundColor(resource)
                }
            })
    }

    private fun obtainBackgroundColor(resource: Bitmap) {
        Palette.from(resource).generate { palette ->
            if (palette == null) {
                return@generate
            }
            var maxPopulation = 0
            var bodyColor = 0
            for (swatch in palette.swatches) {
                if (swatch.population > maxPopulation) {
                    maxPopulation = swatch.population
                    bodyColor = swatch.rgb
                }
            }
            ViewCompat.setBackgroundTintList(coupon_image, ColorStateList.valueOf(bodyColor))
        }
    }
}

data class Coupon(
    val id: Int,
    val title: String,
    val brand: String,
    val startDate: String,
    val endDate: String,
    val remain: Int,
    val linkUrl: String,
    val imageUrl: String
) : DelegateAdapter.UiModel()
