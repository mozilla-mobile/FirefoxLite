package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSource
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.sha256
import org.mozilla.rocket.util.toJsonObject
import java.net.URLEncoder
import java.util.Locale

class DailyHuntNewsRemoteDataSource(private val appContext: Context, private val newsProvider: DailyHuntProvider?) : NewsDataSource {

    override suspend fun getNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val partner = newsProvider?.partnerCode ?: ""
                val timestamp = System.currentTimeMillis().toString()
                val uid = newsProvider?.userId ?: ""
                sendHttpRequest(
                    request = Request(
                        url = getApiEndpoint(partner, timestamp, uid, category, language, pages, pageSize),
                        method = Request.Method.GET,
                        headers = createApiHeaders(partner, timestamp, uid, category, language, pages, pageSize)
                    ),
                    onSuccess = {
                        Result.Success(fromJson(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get news items ($category, $language, $pages, $pageSize)"
        )
    }

    private fun getApiEndpoint(
        partner: String,
        timestamp: String,
        uid: String,
        category: String,
        languageCode: String,
        pages: Int,
        pageSize: Int
    ): String {
        return String.format(Locale.US, DEFAULT_URL, partner, timestamp, uid, category, languageCode, pages, pageSize)
    }

    private fun createApiHeaders(
        partner: String,
        timestamp: String,
        uid: String,
        category: String,
        languageCode: String,
        pages: Int,
        pageSize: Int
    ) = MutableHeaders().apply {
        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val params = mapOf(
                "partner" to partner,
                "ts" to timestamp,
                "puid" to uid,
                "cid" to category,
                "langCode" to languageCode,
                "pageNumber" to pages.toString(),
                "pageSize" to pageSize.toString(),
                "pfm" to "0",
                "fm" to "0",
                "fields" to "none"
            )
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.GET.name, params)
            set("Signature", signature)
        }
    }

    private fun fromJson(jsonString: String): List<NewsItem> {
        val jsonObject = jsonString.toJsonObject()
        val newsArray = jsonObject.optJSONObject("data").optJSONArray("rows")
        val targetImageDimension = appContext.resources.getDimensionPixelSize(R.dimen.item_news_inner_width).toString()
        return (0 until newsArray.length())
            .map { index ->
                val item = newsArray.getJSONObject(index)
                val imageUrl = try {
                    item.optJSONArray("images")?.getString(0)?.run {
                        this.replace("{CMD}", "crop")
                            .replace("{W}", targetImageDimension)
                            .replace("{H}", targetImageDimension)
                            .replace("{Q}", "75")
                            .replace("{EXT}", "webp")
                    } ?: ""
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }

                val linkUrl = if (item.optString("deepLinkUrl").isNotEmpty()) {
                    item.optString("deepLinkUrl") + "&puid=${URLEncoder.encode(newsProvider?.userId, "UTF-8")}"
                } else {
                    ""
                }

                NewsItem(
                    item.optString("title"),
                    linkUrl,
                    imageUrl,
                    item.optString("source"),
                    item.optLong("publishTime"),
                    linkUrl.sha256(),
                    feed = "dailyhunt"
                )
            }
    }

    companion object {
        private const val DEFAULT_URL = "http://feed.dailyhunt.in/api/v2/syndication/items?partner=%s&ts=%s&puid=%s&cid=%s&langCode=%s&pageNumber=%d&pageSize=%d&pfm=0&fm=0&fields=none"
    }
}