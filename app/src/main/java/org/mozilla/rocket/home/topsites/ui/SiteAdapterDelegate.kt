package org.mozilla.rocket.home.topsites.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.StrictMode
import android.view.View
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.item_top_site.content_image
import kotlinx.android.synthetic.main.item_top_site.pin_indicator
import kotlinx.android.synthetic.main.item_top_site.text
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.strictmodeviolator.StrictModeViolation

class SiteAdapterDelegate(private val homeViewModel: HomeViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            SiteViewHolder(view, homeViewModel)
}

class SiteViewHolder(
    override val containerView: View,
    private val homeViewModel: HomeViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val site = uiModel as Site
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

        // Background color
        val backgroundColor = when (site) {
            is Site.FixedSite -> Color.WHITE
            is Site.RemovableSite -> calculateBackgroundColor(favicon)
        }
        ViewCompat.setBackgroundTintList(content_image, ColorStateList.valueOf(backgroundColor))

        // Pin
        PinViewWrapper(pin_indicator).run {
            visibility = when (site) {
                is Site.FixedSite -> View.GONE
                is Site.RemovableSite -> if (site.isPinned) View.VISIBLE else View.GONE
            }
            setPinColor(backgroundColor)
        }

        itemView.setOnClickListener { homeViewModel.onTopSiteClicked(site, adapterPosition) }
        itemView.setOnLongClickListener {
            it.tag = TOP_SITE_LONG_CLICK_TARGET
            homeViewModel.onTopSiteLongClicked(site)
        }
    }

    private fun getFavicon(context: Context, site: Site): Bitmap {
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

    private fun calculateBackgroundColor(favicon: Bitmap): Int {
        val dominantColor = FavIconUtils.getDominantColor(favicon)
        val alpha = dominantColor and -0x1000000
        // Add 25% white to dominant Color
        val red = addWhiteToColorCode(dominantColor and 0x00FF0000 shr 16, 0.25f) shl 16
        val green = addWhiteToColorCode(dominantColor and 0x0000FF00 shr 8, 0.25f) shl 8
        val blue = addWhiteToColorCode(dominantColor and 0x000000FF, 0.25f)
        return alpha + red + green + blue
    }

    private fun addWhiteToColorCode(colorCode: Int, percentage: Float): Int {
        var result = (colorCode + 0xFF * percentage / 2).toInt()
        if (result > 0xFF) {
            result = 0xFF
        }
        return result
    }

    companion object {
        const val TOP_SITE_LONG_CLICK_TARGET = "top_site_long_click_target"
    }
}

sealed class Site(
    open val id: Long,
    open val title: String,
    open val url: String,
    open val iconUri: String?,
    open val viewCount: Long,
    open val lastViewTimestamp: Long
) : DelegateAdapter.UiModel() {

    data class FixedSite(
        override val id: Long,
        override val title: String,
        override val url: String,
        override val iconUri: String?,
        override val viewCount: Long,
        override val lastViewTimestamp: Long
    ) : Site(id, title, url, iconUri, viewCount, lastViewTimestamp)

    data class RemovableSite(
        override val id: Long,
        override val title: String,
        override val url: String,
        override val iconUri: String?,
        override val viewCount: Long,
        override val lastViewTimestamp: Long,
        val isDefault: Boolean,
        val isPinned: Boolean
    ) : Site(id, title, url, iconUri, viewCount, lastViewTimestamp)
}

fun Site.toSiteModel(): org.mozilla.focus.history.model.Site =
        org.mozilla.focus.history.model.Site(
                id,
                title,
                url,
                viewCount,
                lastViewTimestamp,
                iconUri
        ).apply {
            isDefault = when (this@toSiteModel) {
                is Site.FixedSite -> true
                is Site.RemovableSite -> this@toSiteModel.isDefault
            }
        }