package org.mozilla.rocket.content.games.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.ui.GamesViewModel
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter

class CarouselBannerAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CarouselBannerViewHolder(view, gamesViewModel)
}

class CarouselBannerViewHolder(
    override val containerView: View,
    viewModel: GamesViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = CarouselBannerAdapter(object : CarouselBannerAdapter.EventListener {
        override fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
            viewModel.onBannerItemClicked(bannerItem)
        }
    })

    init {
        carousel_list.adapter = this@CarouselBannerViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as CarouselBanner
        adapter.setData(uiModel.banners)
    }
}

data class CarouselBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : DelegateAdapter.UiModel()