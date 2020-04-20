package org.mozilla.rocket.content.news.data

import android.content.Context
import androidx.paging.DataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.newspoint.NewsPointNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssNewsRemoteDataSource

class NewsDataSourceFactory(
    private val appContext: Context
) : DataSource.Factory<Int, NewsItem>() {

    lateinit var category: String
    lateinit var language: String

    override fun create(): DataSource<Int, NewsItem> {
        val newsProvider = NewsProvider.getNewsProvider()
        return if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                DailyHuntNewsRemoteDataSource(appContext, dailyHuntProvider, category, language)
            } else {
                NewsPointNewsRemoteDataSource(newsProvider, category, language)
            }
        } else {
            RssNewsRemoteDataSource(newsProvider, category)
        }
    }
}