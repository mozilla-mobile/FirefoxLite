package org.mozilla.rocket.content.news.data.newspoint

import androidx.paging.PageKeyedDataSource
import mozilla.components.concept.fetch.Request
import org.json.JSONObject
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSourceFactory.PageKey
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsProvider
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.sha256
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class NewsPointNewsRemoteDataSource(
    private val newsProvider: NewsProvider?,
    private val category: String,
    private val language: String
) : PageKeyedDataSource<PageKey, NewsItem>() {

    override fun loadInitial(params: LoadInitialParams<PageKey>, callback: LoadInitialCallback<PageKey, NewsItem>) {
        val pageSize = params.requestedLoadSize
        val pages = 1

        val result = fetchNewsItems(category, language, pageSize, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, null, PageKey.PageNumberKey(2))
        } // TODO: error handling
    }

    override fun loadBefore(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pageSize = params.requestedLoadSize
        val pages = (params.key as PageKey.PageNumberKey).number

        val result = fetchNewsItems(category, language, pageSize, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, PageKey.PageNumberKey(pages - 1))
        } // TODO: error handling
    }

    override fun loadAfter(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pageSize = params.requestedLoadSize
        val pages = (params.key as PageKey.PageNumberKey).number

        val result = fetchNewsItems(category, language, pageSize, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, PageKey.PageNumberKey(pages + 1))
        } // TODO: error handling
    }

    private fun fetchNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>> {
        return sendHttpRequest(
            request = Request(
                url = getApiEndpoint(category, language, pages, pageSize),
                method = Request.Method.GET
            ),
            onSuccess = {
                Result.Success(fromJson(it.body.string()))
            },
            onError = {
                Result.Error(it)
            }
        )
    }

    private fun getApiEndpoint(category: String, language: String, pages: Int, pageSize: Int): String {
        val url = newsProvider?.newsUrl ?: DEFAULT_URL
        return String.format(Locale.US, url, category, language, pages, pageSize)
    }

    private fun fromJson(jsonString: String): List<NewsItem> {
        val newsList = ArrayList<NewsItem>()
        val items = JSONObject(jsonString).getJSONArray("items")
        for (i in 0 until items.length()) {
            val jsonObject = items.getJSONObject(i)
            val id = jsonObject.optString("id")
            val title = jsonObject.optString("hl")
            val link = jsonObject.optString("mwu")
            val imageUrl = jsonObject.optJSONArray("images")?.getString(0) ?: ""
            val source = jsonObject.optString("pn")
            val publishDate = jsonObject.optString("dl")
            if (id == null || title == null || link == null || publishDate == null) {
                continue
            }
            var publishTime: Long
            try {
                publishTime = SimpleDateFormat("EEE MMM dd HH:mm:ss 'IST' yyyy", Locale.US).parse(publishDate).time
            } catch (e: ParseException) {
                e.printStackTrace()
                // skip this item
                continue
            }

            newsList.add(NewsItem(title, link, imageUrl, source, publishTime, link.sha256(), feed = "newspoint"))
        }
        return newsList
    }

    companion object {
        private const val DEFAULT_URL = "http://partnersnp.indiatimes.com/feed/fx/atp?channel=*&section=%s&lang=%s&curpg=%s&pp=%s&v=v1&fromtime=1551267146210"
    }
}