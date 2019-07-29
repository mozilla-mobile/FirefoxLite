package org.mozilla.rocket.vertical.games.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.vertical.games.GamesViewModel
import org.mozilla.rocket.vertical.games.GamesViewModel.BannerItem

class CarouselBannerAdapter(
    private val gamesViewModel: GamesViewModel
) : RecyclerView.Adapter<CarouselBannerAdapter.BannerViewHolder>() {

    private var data = mutableListOf<BannerItem>()

    fun setData(data: List<BannerItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view).apply {
            setOnItemClickListener { gamesViewModel.onBannerItemClicked(it) }
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(data[position])
    }

    class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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