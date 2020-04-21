package org.mozilla.rocket.content.news.data.rss

import androidx.paging.PageKeyedDataSource
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.mozilla.focus.locale.Locales
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSourceFactory.PageKey
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsProvider
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.sha256
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RssNewsRemoteDataSource(
    private val newsProvider: NewsProvider?,
    private val category: String
) : PageKeyedDataSource<PageKey, NewsItem>() {

    override fun loadInitial(params: LoadInitialParams<PageKey>, callback: LoadInitialCallback<PageKey, NewsItem>) {
        val pages = 1

        val result = fetchNewsItems(category, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, null, PageKey.PageNumberKey(2))
        } // TODO: error handling
    }

    override fun loadBefore(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pages = (params.key as PageKey.PageNumberKey).number

        val result = fetchNewsItems(category, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, PageKey.PageNumberKey(pages - 1))
        } // TODO: error handling
    }

    override fun loadAfter(params: LoadParams<PageKey>, callback: LoadCallback<PageKey, NewsItem>) {
        val pages = (params.key as PageKey.PageNumberKey).number

        val result = fetchNewsItems(category, pages)
        if (result is Result.Success) {
            callback.onResult(result.data, PageKey.PageNumberKey(pages + 1))
        } // TODO: error handling
    }

    private fun fetchNewsItems(category: String, pages: Int): Result<List<NewsItem>> {
        return if (pages != 1) {
            Result.Error(Exception("No pagination support for the RSS news"))
        } else {
            sendHttpRequest(
                request = Request(url = getApiEndpoint(category), method = Request.Method.GET),
                onSuccess = {
                    Result.Success(fromJson(it.body.string()))
                },
                onError = {
                    Result.Error(it)
                }
            )
        }
    }

    private fun getApiEndpoint(category: String): String {
        val url = newsProvider?.newsUrl ?: DEFAULT_URL
        return String.format(
            Locale.US,
            url,
            category,
            Locales.getLanguage(Locale.getDefault()),
            Locale.getDefault().country,
            Locale.getDefault().country + ":" + Locales.getLanguage(Locale.getDefault())
        )
    }

    private fun fromJson(jsonString: String): List<NewsItem> {
        val newsList = ArrayList<NewsItem>()
        val items = JSONArray(jsonString)
        for (i in 0 until items.length()) {
            val jsonObject = items.getJSONObject(i)
            val title = jsonObject.optString("title")
            val link = jsonObject.optString("link")
            val imageUrl: String? = jsonObject.optString("image")?.let {
                if (it == "null") {
                    null
                } else {
                    it
                }
            }
            val source = jsonObject.optString("source")
            val publishDate = jsonObject.optString("pubDate")
            if (title == null || link == null || publishDate == null) {
                continue
            }
            val publishTime = try {
                SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).parse(publishDate).time
            } catch (e: ParseException) {
                e.printStackTrace()
                Long.MIN_VALUE
            }

            newsList.add(NewsItem(title, link, imageUrl, source, publishTime, link.sha256()))
        }
        return newsList
    }

    companion object {
        private const val DEFAULT_URL = "https://zerda-dcf76.appspot.com/api/v1/news/google/topic/%s?hl=%s&gl=%s&ceid=%s"
    }
}