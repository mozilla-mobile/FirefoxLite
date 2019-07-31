package org.mozilla.rocket.content.games.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter

class CarouselBannerAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CarouselBannerViewHolder(view)
}

class CarouselBannerViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = CarouselBannerAdapter(object : CarouselBannerAdapter.EventListener {
        override fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
            // TODO
        }
    })

    init {
        carousel_list.adapter = this@CarouselBannerViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UIModel) {
        uiModel as CarouselBanner
        adapter.setData(uiModel.banners)
    }
}

data class CarouselBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : DelegateAdapter.UIModel()