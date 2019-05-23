package org.mozilla.rocket.content.ecommerce

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.ecommerce.data.Coupon
import java.text.SimpleDateFormat
import java.util.Locale

class CouponAdapter : ListAdapter<Coupon, CouponViewHolder>(
    COMPARATOR
) {
    object COMPARATOR : DiffUtil.ItemCallback<Coupon>() {
        override fun areItemsTheSame(oldItem: Coupon, newItem: Coupon): Boolean {
            return oldItem.link.url == newItem.link.url
        }

        override fun areContentsTheSame(oldItem: Coupon, newItem: Coupon): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_coupon, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, position)
    }
}

class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var view: View? = null
    var image: ImageView? = null
    var name: TextView? = null
    var validPeriod: TextView? = null

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    init {
        view = itemView.findViewById(R.id.coupon_item)
        image = itemView.findViewById(R.id.coupon_item_image)
        name = itemView.findViewById(R.id.coupon_item_headline)
        validPeriod = itemView.findViewById(R.id.coupon_item_time)
    }

    fun bind(item: Coupon, position: Int) {
        view?.setOnClickListener {
            ScreenNavigator.get(it.context).showBrowserScreen(item.link.url, true, false)
            TelemetryWrapper.clickOnPromoItem(
                pos = position.toString(),
                id = item.id,
                feed = item.feed,
                source = item.link.source,
                category = item.category,
                subcategory = item.subcategory
            )
        }

        GlideApp.with(itemView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(item.link.image)
                .into(image)

        name?.text = item.link.name

        val validPeriodStr = toValidPeriodFormat(item.end)
        if (validPeriodStr.isNotEmpty()) {
            validPeriod?.text = validPeriodStr
            validPeriod?.visibility = View.VISIBLE
        } else {
            validPeriod?.visibility = View.GONE
        }
    }

    private fun toValidPeriodFormat(end: Long): String {
        if (end == 0L) {
            return ""
        }

        return itemView.context.getString(R.string.coupon_valid_description, dateFormat.format(end))
    }
}
