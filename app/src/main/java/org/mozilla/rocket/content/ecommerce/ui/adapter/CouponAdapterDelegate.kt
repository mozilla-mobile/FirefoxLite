package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.graphics.Bitmap
import android.view.View
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
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
        val remainFormat = itemView.context.getString(R.string.shopping_coupon_expire_other)
        coupon_remain.text = String.format(remainFormat, couponItem.remain)

        GlideApp.with(itemView.context)
            .asBitmap()
            .load(couponItem.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.news_item_img_bg)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    if (resource != null) {
                        coupon_image.setBackgroundColor(obtainBackgroundColor(resource))
                    }
                    return false
                }
            })
            .into(coupon_image)
    }

    private fun obtainBackgroundColor(resource: Bitmap): Int {
        val palette = Palette.from(resource).generate()
        var maxPopulation = 0
        var bodyColor = 0
        for (swatch in palette.swatches) {
            if (swatch.population > maxPopulation) {
                maxPopulation = swatch.population
                bodyColor = swatch.rgb
            }
        }
        return bodyColor
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
