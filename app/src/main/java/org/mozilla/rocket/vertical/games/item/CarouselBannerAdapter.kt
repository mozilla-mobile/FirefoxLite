package org.mozilla.rocket.vertical.games.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.vertical.games.GamesViewModel
import org.mozilla.rocket.vertical.games.GamesViewModel.BannerItem

class CarouselBannerAdapter(
    private val gamesViewModel: GamesViewModel
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
            setOnItemClickListener { gamesViewModel.onBannerItemClicked(it) }
            bind(data[position])
        }
        container.addView(view)

        return viewHolder
    }

    override fun getCount(): Int = data.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        `object` as BannerViewHolder
        return view === `object`.itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        `object` as BannerViewHolder
        container.removeView(`object`.itemView)
    }

    class BannerViewHolder(val itemView: View) {
        // TODO: use kotlinx
        val image: ImageView = itemView.findViewById(R.id.image)

        private var onItemClickListener: ((BannerItem) -> Unit)? = null

        fun bind(bannerItem: BannerItem) {
            GlideApp.with(itemView.context)
                    .asBitmap()
                    .placeholder(R.drawable.placeholder)
                    .fitCenter()
                    .load(bannerItem.imageUrl)
                    .into(image)

            itemView.setOnClickListener { onItemClickListener?.invoke(bannerItem) }
        }

        fun setOnItemClickListener(listener: (BannerItem) -> Unit) {
            onItemClickListener = listener
        }
    }
}