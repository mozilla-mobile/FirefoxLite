package org.mozilla.rocket.content.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_banner.image
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp

class CarouselBannerAdapter(
    private val eventListener: EventListener
) : PagerAdapter() {

    private var data = mutableListOf<BannerItem>()

    fun setData(data: List<BannerItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.item_banner, container, false)
        val viewHolder = BannerViewHolder(view).apply {
            setOnItemClickListener { eventListener.onBannerItemClicked(it) }
            bind(data[position])
        }
        container.addView(view)

        return viewHolder
    }

    override fun getCount(): Int = data.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        `object` as BannerViewHolder
        return view === `object`.containerView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        `object` as BannerViewHolder
        container.removeView(`object`.containerView)
    }

    class BannerViewHolder(override val containerView: View) : LayoutContainer {
        private var onItemClickListener: ((BannerItem) -> Unit)? = null

        fun bind(bannerItem: BannerItem) {
            GlideApp.with(containerView.context)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .fitCenter()
                .load(bannerItem.imageUrl)
                .into(image)

            containerView.setOnClickListener { onItemClickListener?.invoke(bannerItem) }
        }

        fun setOnItemClickListener(listener: (BannerItem) -> Unit) {
            onItemClickListener = listener
        }
    }

    data class BannerItem(
        val id: String,
        val imageUrl: String,
        val linkUrl: String
    )

    interface EventListener {
        fun onBannerItemClicked(bannerItem: BannerItem)
    }
}