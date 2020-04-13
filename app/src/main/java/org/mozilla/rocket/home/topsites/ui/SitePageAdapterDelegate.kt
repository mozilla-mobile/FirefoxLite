package org.mozilla.rocket.home.topsites.ui

import android.view.View
import kotlinx.android.synthetic.main.item_top_site_page.page_list
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.home.HomeViewModel

class SitePageAdapterDelegate(
    private val homeViewModel: HomeViewModel,
    private val chromeViewModel: ChromeViewModel,
    private val specifiedFaviconBgColors: List<FaviconBgColor>?
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            SitePageViewHolder(view, homeViewModel, chromeViewModel, specifiedFaviconBgColors)
}

class SitePageViewHolder(
    override val containerView: View,
    homeViewModel: HomeViewModel,
    chromeViewModel: ChromeViewModel,
    specifiedFaviconBgColors: List<FaviconBgColor>?
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(Site.UrlSite.FixedSite::class, R.layout.item_top_site, SiteAdapterDelegate(homeViewModel, chromeViewModel, specifiedFaviconBgColors))
            add(Site.UrlSite.RemovableSite::class, R.layout.item_top_site, SiteAdapterDelegate(homeViewModel, chromeViewModel, specifiedFaviconBgColors))
        }
    )

    init {
        page_list.adapter = this@SitePageViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as SitePage
        adapter.setData(uiModel.sites)
    }
}

data class SitePage(val sites: List<Site>) : DelegateAdapter.UiModel()