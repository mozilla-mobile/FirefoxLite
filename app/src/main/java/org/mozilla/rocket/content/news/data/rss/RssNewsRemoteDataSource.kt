package org.mozilla.rocket.content.news.data.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsDataSource
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.util.safeApiCall
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RssNewsRemoteDataSource : NewsDataSource {

    override suspend fun getNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                if (pages != 1) {
                    Result.Error(Exception("No pagination support for the RSS news"))
                } else {
                    val responseBody = getHttpResult(getApiEndpoint(category))
                    Result.Success(fromJson(responseBody))
                }
            },
            errorMessage = "Unable to get news items ($category, $language, $pages, $pageSize)"
        )
    }

    private fun getHttpResult(endpointUrl: String): String {
        var responseBody = HttpRequest.get(URL(endpointUrl), "")
        responseBody = responseBody.replace("\n", "")
        return responseBody
    }

    private fun getApiEndpoint(category: String): String {
        return String.format(Locale.getDefault(), DEFAULT_URL, category, Locale.getDefault().toLanguageTag(), Locale.getDefault().country)
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
            var publishTime: Long
            try {
                publishTime = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault()).parse(publishDate).time
            } catch (e: ParseException) {
                e.printStackTrace()
                // skip this item
                continue
            }

            newsList.add(NewsItem(title, link, imageUrl, source, publishTime))
        }
        return newsList
    }

    companion object {
        private const val DEFAULT_URL = "https://rocket-dev01.appspot.com/api/v1/news/google/topic/%s?language=%s&country=%s"
    }
}