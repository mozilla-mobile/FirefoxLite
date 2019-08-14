package org.mozilla.rocket.content.ecommerce.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.ecommerce.ShoppingViewModel

class CouponBannerAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CouponBannerViewHolder(view, shoppingViewModel)
}

class CouponBannerViewHolder(
    override val containerView: View,
    shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = CarouselBannerAdapter(object : CarouselBannerAdapter.EventListener {
        override fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
            shoppingViewModel.onBannerItemClicked(itemView.context, bannerItem)
        }
    })

    init {
        carousel_list.adapter = this@CouponBannerViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as CouponBanner
        adapter.setData(uiModel.banners)
    }
}

data class CouponBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : DelegateAdapter.UiModel()