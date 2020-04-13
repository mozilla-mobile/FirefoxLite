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
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.getJsonArray
import org.mozilla.strictmodeviolator.StrictModeViolation
import java.net.URI
import java.net.URISyntaxException

class SiteAdapterDelegate(
    private val homeViewModel: HomeViewModel,
    private val chromeViewModel: ChromeViewModel,
    private val specifiedFaviconBgColors: List<FaviconBgColor>?
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            SiteViewHolder(view, homeViewModel, chromeViewModel, specifiedFaviconBgColors)
}

class SiteViewHolder(
    override val containerView: View,
    private val homeViewModel: HomeViewModel,
    private val chromeViewModel: ChromeViewModel,
    private val specifiedFaviconBgColors: List<FaviconBgColor>?
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val site = uiModel as Site
        when (site) {
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

                // Background color
                val backgroundColor = when (site) {
                    is Site.UrlSite.FixedSite -> Color.WHITE
                    is Site.UrlSite.RemovableSite -> getBackgroundColor(site.url, favicon)
                }
                ViewCompat.setBackgroundTintList(content_image, ColorStateList.valueOf(backgroundColor))

                // Pin
                PinViewWrapper(pin_indicator).run {
                    visibility = when (site) {
                        is Site.UrlSite.FixedSite -> View.GONE
                        is Site.UrlSite.RemovableSite -> if (site.isPinned) View.VISIBLE else View.GONE
                    }
                    setPinColor(backgroundColor)
                }

                itemView.setOnClickListener { homeViewModel.onTopSiteClicked(site, adapterPosition) }
                if (site is Site.UrlSite.FixedSite) {
                    itemView.setOnLongClickListener(null)
                } else {
                    itemView.setOnLongClickListener {
                        it.tag = TOP_SITE_LONG_CLICK_TARGET
                        homeViewModel.onTopSiteLongClicked(site, adapterPosition)
                    }
                }
            }
        }
        text.setNightMode(chromeViewModel.isNightMode.value?.isEnabled == true)
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

    private fun getBackgroundColor(url: String, bitmap: Bitmap): Int =
            findSpecifiedBgColor(url) ?: calculateBackgroundColor(bitmap)

    private fun findSpecifiedBgColor(url: String): Int? {
        return try {
            val host = URI(url).host
            specifiedFaviconBgColors
                    ?.find { it.host == host }
                    ?.bg_color
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }
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

sealed class Site : DelegateAdapter.UiModel() {
    sealed class UrlSite(
        open val id: Long,
        open val title: String,
        open val url: String,
        open val iconUri: String?,
        open val viewCount: Long,
        open val lastViewTimestamp: Long
    ) : Site() {
        data class FixedSite(
            override val id: Long,
            override val title: String,
            override val url: String,
            override val iconUri: String?,
            override val viewCount: Long,
            override val lastViewTimestamp: Long
        ) : UrlSite(id, title, url, iconUri, viewCount, lastViewTimestamp)

        data class RemovableSite(
            override val id: Long,
            override val title: String,
            override val url: String,
            override val iconUri: String?,
            override val viewCount: Long,
            override val lastViewTimestamp: Long,
            val isDefault: Boolean,
            val isPinned: Boolean
        ) : UrlSite(id, title, url, iconUri, viewCount, lastViewTimestamp)
    }
}

fun Site.UrlSite.toSiteModel(): org.mozilla.focus.history.model.Site =
        org.mozilla.focus.history.model.Site(
            id,
            title,
            url,
            viewCount,
            lastViewTimestamp,
            iconUri
        ).apply {
            isDefault = when (this@toSiteModel) {
                is Site.UrlSite.FixedSite -> true
                is Site.UrlSite.RemovableSite -> this@toSiteModel.isDefault
            }
        }

data class FaviconBgColor(val host: String, val bg_color: Int) {
    companion object {
        const val KEY_HOST = "host"
        const val KEY_BG_COLOR = "bg_color"
    }
}

fun getFaviconBgColorsFromResource(appContext: Context): List<FaviconBgColor>? =
        AssetsUtils.loadStringFromRawResource(appContext, R.raw.favicon_bg_colors)
                ?.jsonStringToFaviconBgColors()

private fun String.jsonStringToFaviconBgColors(): List<FaviconBgColor>? {
    return try {
        this.getJsonArray { paresFaviconBgColor(it) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    } catch (e: NumberFormatException) {
        e.printStackTrace()
        null
    }
}

@Throws(JSONException::class, NumberFormatException::class)
private fun paresFaviconBgColor(jsonObject: JSONObject): FaviconBgColor {
    val host = jsonObject.getString(FaviconBgColor.KEY_HOST)
    val bgColorStr = jsonObject.getString(FaviconBgColor.KEY_BG_COLOR)
    val bgColor = java.lang.Long.parseLong(bgColorStr, 16).toInt()

    return FaviconBgColor(host, bgColor)
}