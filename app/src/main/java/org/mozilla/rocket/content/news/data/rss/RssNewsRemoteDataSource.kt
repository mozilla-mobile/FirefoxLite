package org.mozilla.rocket.content.news.data.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSource
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsProvider
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RssNewsRemoteDataSource(private val newsProvider: NewsProvider?) : NewsDataSource {

    override suspend fun getNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                if (pages != 1) {
                    Result.Error(Exception("No pagination support for the RSS news"))
                } else {
                    sendHttpRequest(request = Request(url = getApiEndpoint(category), method = Request.Method.GET),
                        onSuccess = {
                            Result.Success(fromJson(it.body.string()))
                        },
                        onError = {
                            Result.Error(it)
                        }
                    )
                }
            },
            errorMessage = "Unable to get news items ($category, $language, $pages, $pageSize)"
        )
    }

    private fun getApiEndpoint(category: String): String {
        val url = newsProvider?.newsUrl ?: DEFAULT_URL
        return String.format(
            Locale.US,
            url,
            category,
            Locale.getDefault().language,
            Locale.getDefault().country,
            Locale.getDefault().country + ":" + Locale.getDefault().language
        )
    }

    private fun fromJson(jsonString: String): List<NewsItem> {
        val newsList = ArrayList<NewsItem>()
        val items = JSONArray(jsonString)
        for (i in 0 until items.length()) {
            val jsonObject = items.getJSONObject(i)
            val title = jsonObject.optString("title")
            val link = jsonObject.optString("link")
            val imageUrl = jsonObject.optString("image")
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

            newsList.add(NewsItem(title, link, imageUrl, source, publishTime))
        }
        return newsList
    }

    companion object {
        private const val DEFAULT_URL = "https://rocket-dev01.appspot.com/api/v1/news/google/topic/%s?hl=%s&gl=%s&ceid=%s"
    }
}