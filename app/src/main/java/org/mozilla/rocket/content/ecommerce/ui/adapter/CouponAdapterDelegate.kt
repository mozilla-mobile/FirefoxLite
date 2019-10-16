package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.graphics.Bitmap
import android.view.View
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import kotlinx.android.synthetic.main.item_coupon.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.CouponViewModel
import org.mozilla.rocket.extension.obtainBackgroundColor

class CouponAdapterDelegate(private val couponViewModel: CouponViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CouponViewHolder(view, couponViewModel)
}

class CouponViewHolder(
    override val containerView: View,
    private val couponViewModel: CouponViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val couponItem = uiModel as Coupon

        itemView.setOnClickListener { couponViewModel.onCouponItemClicked(couponItem) }

        coupon_brand.text = couponItem.source
        coupon_title.text = couponItem.title
        val remainFormat = itemView.context.getString(R.string.shopping_coupon_expire_other)
        coupon_remain.text = String.format(remainFormat, couponItem.endDate)

        GlideApp.with(itemView.context)
            .asBitmap()
            .load(couponItem.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    if (resource != null) {
                        coupon_image.setBackgroundColor(resource.obtainBackgroundColor())
                    }
                    return false
                }
            })
            .into(coupon_image)
    }
}

data class Coupon(
    val source: String,
    val imageUrl: String,
    val linkUrl: String,
    val title: String,
    val componentId: String,
    val endDate: Long
) : DelegateAdapter.UiModel()
