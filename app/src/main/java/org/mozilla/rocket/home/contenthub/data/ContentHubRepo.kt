package org.mozilla.rocket.home.contenthub.data

import android.content.Context
import org.json.JSONException
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray

class ContentHubRepo(private val appContext: Context) {

    fun getConfiguredContentHubItems(): List<ContentHubItem>? =
            FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_CONTENT_HUB_ITEMS)
                    .takeIf { it.isNotEmpty() }
                    ?.jsonStringToContentHubItems()

    fun getDefaultContentHubItems(): List<ContentHubItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.content_hub_default_items)
                    ?.jsonStringToContentHubItems()
}

private fun String.jsonStringToContentHubItems(): List<ContentHubItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> jsonObject.getInt("type") }
                .map { type -> createContentHubItem(type) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

sealed class ContentHubItem(val iconResId: Int) {
    class Travel : ContentHubItem(R.drawable.ic_travel)
    class Shopping : ContentHubItem(R.drawable.ic_shopping)
    class News : ContentHubItem(R.drawable.ic_news)
    class Games : ContentHubItem(R.drawable.ic_games)
}

private fun createContentHubItem(type: Int): ContentHubItem =
        when (type) {
            1 -> ContentHubItem.Travel()
            2 -> ContentHubItem.Shopping()
            3 -> ContentHubItem.News()
            4 -> ContentHubItem.Games()
            else -> error("Unsupported content hub item type $type")
        }