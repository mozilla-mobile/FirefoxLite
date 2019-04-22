package org.mozilla.rocket.content

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.data.Coupon
import java.text.SimpleDateFormat
import java.util.*

class CouponAdapter(private val listener: ContentPortalListener) : ListAdapter<Coupon, CouponViewHolder>(
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
        holder.bind(item, View.OnClickListener {
            TelemetryWrapper.clickOnEcItem(
                    pos = position.toString(),
                    source = item.link.source,
                    category = item.link.name
            )
            listener.onItemClicked(item.link.url)
        })
    }
}

class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var view: View? = null
    var image: ImageView? = null
    var name: TextView? = null
    var validPeriod: TextView? = null

    private val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

    init {
        view = itemView.findViewById(R.id.coupon_item)
        image = itemView.findViewById(R.id.coupon_item_image)
        name = itemView.findViewById(R.id.coupon_item_headline)
        validPeriod = itemView.findViewById(R.id.coupon_item_time)
    }

    fun bind(item: Coupon, listener: View.OnClickListener) {
        view?.setOnClickListener(listener)
        name?.text = item.link.name
        validPeriod?.text = toValidPeriodFormat(item.start, item.end)
    }

    private fun toValidPeriodFormat(start: Long, end: Long): String {
        val startCalendar = Calendar.getInstance()
        startCalendar.timeInMillis = start

        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = end

        val startStr =
                if (startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)
                        && startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)) {
                    startCalendar.get(Calendar.DAY_OF_MONTH).toString()
                } else {
                    dateFormat.format(start)
                }

        val endStr = dateFormat.format(end)

        return String.format("Valid period %s - %s", startStr, endStr)
    }
}