package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import android.net.Uri
import androidx.paging.PageKeyedDataSource
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSourceFactory.PageKey
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.sha256
import org.mozilla.rocket.util.toJsonObject
import java.net.URLEncoder

class DailyHuntNewsRemoteDataSource(
    private val appContext: Context,
    private val newsProvider: DailyHuntProvider?,
    private val category: String,
    private val language: String
) : PageKeyedDataSource<PageKey, NewsItem>() {

    override fun loadInitial(params: LoadInitialParams<PageKey>, callback: LoadInitialCallback<PageKey, NewsItem>) {
        val pageSize = params.requestedLoadSize

        val result = fetchNewsItems(newsProvider, category, language,
                pageSize, 0)
        if (result is Result.Success) {
            val (nextPageKey, items) = result.data
            callback.onResult(items, null, nextPageKey)
        } // TODO: error handling
    }

    override fun loadBefore(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        // Do nothing
    }

    override fun loadAfter(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pageKey = params.key as PageKey.PageUrlKey

        val result = fetchNewsItemsNextPage(pageKey.url)
        if (result is Result.Success) {
            val (nextPageKey, items) = result.data
            callback.onResult(items, nextPageKey)
        } // TODO: error handling
    }

    private fun fetchNewsItems(
        newsProvider: DailyHuntProvider?,
        category: String,
        language: String,
        pageSize: Int,
        pages: Int
    ): Result<Pair<PageKey.PageUrlKey, List<NewsItem>>> {
        val params = createApiParams(
            partner = newsProvider?.partnerCode ?: "",
            timestamp = System.currentTimeMillis().toString(),
            uid = newsProvider?.userId ?: "",
            category = category,
            languageCode = language,
            pages = pages,
            pageSize = pageSize
        )
        return sendHttpRequest(
            request = Request(
                url = getApiEndpoint(params),
                method = Request.Method.GET,
                headers = createApiHeaders(params)
            ),
            onSuccess = {
                val body = it.body.string()
                val nextPageKey = PageKey.PageUrlKey(parseNextPageUrl(body))
                val items = fromJson(body)
                Result.Success(nextPageKey to items)
            },
            onError = {
                Result.Error(it)
            }
        )
    }

    private fun fetchNewsItemsNextPage(nextPageUrl: String): Result<Pair<PageKey.PageUrlKey, List<NewsItem>>> {
        val params = parseUrlParams(nextPageUrl).toMutableMap().apply {
            put("ts", System.currentTimeMillis().toString())
        }
        return sendHttpRequest(
            request = Request(
                url = getApiEndpoint(params),
                method = Request.Method.GET,
                headers = createApiHeaders(params)
            ),
            onSuccess = {
                val body = it.body.string()
                val nextPageKey = PageKey.PageUrlKey(parseNextPageUrl(body))
                val items = fromJson(body)
                Result.Success(nextPageKey to items)
            },
            onError = {
                Result.Error(it)
            }
        )
    }

    private fun createApiParams(
        partner: String,
        timestamp: String,
        uid: String,
        category: String,
        languageCode: String,
        pageSize: Int,
        pages: Int
    ): Map<String, String> = mapOf(
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

    private fun getApiEndpoint(params: Map<String, String>): String = Uri.parse(API_URL)
            .buildUpon()
            .apply {
                for ((key, value) in params.entries) {
                    appendQueryParameter(key, value)
                }
            }
            .build()
            .toString()

    private fun createApiHeaders(params: Map<String, String>) = MutableHeaders().apply {
        newsProvider?.apiKey?.let {
            set("Authorization", it)
        }

        newsProvider?.secretKey?.let {
            val signature = DailyHuntUtils.generateSignature(it, Request.Method.GET.name, urlEncodeParams(params))
            set("Signature", signature)
        }
    }

    private fun urlEncodeParams(params: Map<String, String>): Map<String, String> {
        val encodedParams = mutableMapOf<String, String>()
        params.forEach {
            encodedParams[it.key] = URLEncoder.encode(it.value, "UTF-8")
        }

        return encodedParams
    }

    private fun parseNextPageUrl(jsonString: String): String = jsonString.toJsonObject()
            .optJSONObject("data")
            ?.optString("nextPageUrl") ?: ""

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
        private const val API_URL = "http://feed.dailyhunt.in/api/v2/syndication/items"
    }
}