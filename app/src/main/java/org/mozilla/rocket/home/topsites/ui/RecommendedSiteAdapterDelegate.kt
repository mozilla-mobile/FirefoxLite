package org.mozilla.rocket.home.topsites.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.StrictMode
import android.view.View
import kotlinx.android.synthetic.main.item_recommended_site.content_image
import kotlinx.android.synthetic.main.item_recommended_site.text
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.strictmodeviolator.StrictModeViolation

class RecommendedSitesAdapterDelegate(
    private val topSiteClickListener: TopSiteClickListener
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        RecommendedSiteViewHolder(view, topSiteClickListener)
}

class RecommendedSiteViewHolder(
    override val containerView: View,
    private val topSiteClickListener: TopSiteClickListener
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        when (val site = uiModel as Site) {
            is Site.UrlSite -> {
                text.text = site.title

                // Tried AsyncTask and other simple offloading, the performance drops significantly.
                // FIXME: 9/21/18 by saving bitmap color, cause FaviconUtils.getDominantColor runs slow.
                // Favicon
                val favicon = StrictModeViolation.tempGrant(
                    { obj: StrictMode.ThreadPolicy.Builder -> obj.permitDiskReads() },
                    { getFavicon(itemView.context, site) }
                )
                content_image.visibility = View.VISIBLE
                content_image.setImageBitmap(favicon)

                itemView.setOnClickListener { topSiteClickListener.onTopSiteClicked(site, adapterPosition) }
            }
        }
    }

    private fun getFavicon(context: Context, site: Site.UrlSite): Bitmap {
        val faviconUri = site.iconUri
        var favicon: Bitmap? = null
        if (faviconUri != null) {
            favicon = FavIconUtils.getBitmapFromUri(context, faviconUri)
        }

        return getBestFavicon(context.resources, site.url, favicon)
    }

    private fun getBestFavicon(res: Resources, url: String, favicon: Bitmap?): Bitmap {
        return when {
            favicon == null -> createFavicon(res, url, Color.WHITE)
            DimenUtils.iconTooBlurry(res, favicon.width) -> createFavicon(res, url, FavIconUtils.getDominantColor(favicon))
            else -> favicon
        }
    }

    private fun createFavicon(resources: Resources, url: String, backgroundColor: Int): Bitmap {
        return DimenUtils.getInitialBitmap(resources, FavIconUtils.getRepresentativeCharacter(url),
            backgroundColor)
    }
}