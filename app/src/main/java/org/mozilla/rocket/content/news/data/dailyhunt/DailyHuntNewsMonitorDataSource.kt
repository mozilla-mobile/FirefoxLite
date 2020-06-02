package org.mozilla.rocket.content.news.data.dailyhunt

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsMonitorDataSource
import org.mozilla.rocket.util.sendHttpRequest
import java.net.URLEncoder

class DailyHuntNewsMonitorDataSource(private val newsProvider: DailyHuntProvider?) : NewsMonitorDataSource {

    override suspend fun trackItemsShown(items: List<NewsItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty() || items[0] !is NewsItem.NewsContentItem) {
            return@withContext
        }
        val params = parseUrlParams((items[0] as NewsItem.NewsContentItem).trackingUrl).toMutableMap().apply {
            put("partner", newsProvider?.partnerCode ?: "")
            put("puid", newsProvider?.userId ?: "")
            put("ts", System.currentTimeMillis().toString())
        }
        sendHttpRequest(
            request = Request(
                url = getTrackingApiEndpoint(params),
                method = Request.Method.POST,
                headers = createTrackingApiHeaders(params),
                body = Request.Body.fromString(createTrackingBody(items))
            ),
            onSuccess = {
                // do noting
            },
            onError = {
                // do noting
            }
        )

        sendHttpRequest(
            request = Request(
                url = (items[0] as NewsItem.NewsContentItem).attributionUrl,
                method = Request.Method.GET
            ),
            onSuccess = {
                // do noting
            },
            onError = {
                // do noting
            }
        )
    }

    private fun parseUrlParams(url: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse(url)
            val args: Set<String> = uri.queryParameterNames
            args.forEach { key ->
                map[key] = uri.getQueryParameter(key) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    private fun getTrackingApiEndpoint(params: Map<String, String>): String = Uri.parse(TRACKING_API_URL)
        .buildUpon()
        .apply {
            for ((key, value) in params.entries) {
                appendQueryParameter(key, value)
            }
        }
        .build()
        .toString()

    private fun createTrackingApiHeaders(params: Map<String, String>) = MutableHeaders().apply {
        set("Content-Type", "application/json")

        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.POST.name, urlEncodeParams(params))
            set("Signature", signature)
        }
    }

    private fun createTrackingBody(items: List<NewsItem>): String {
        val json = JSONObject()
        json.put("viewedDate", System.currentTimeMillis())

        val jsonArray = JSONArray()
        for (item in items) {
            if (item is NewsItem.NewsContentItem) {
                jsonArray.put(
                    JSONObject()
                        .put("id", item.trackingId)
                        .put("trackData", item.trackingData)
                )
            }
        }
        json.put("stories", jsonArray)
        return json.toString()
    }

    private fun urlEncodeParams(params: Map<String, String>): Map<String, String> {
        val encodedParams = mutableMapOf<String, String>()
        params.forEach {
            encodedParams[it.key] = URLEncoder.encode(it.value, "UTF-8")
        }

        return encodedParams
    }

    companion object {
        private const val TRACKING_API_URL = "http://track.dailyhunt.in/api/v2/syndication/tracking"
    }
}