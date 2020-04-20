package org.mozilla.rocket.content.news.data

import android.content.Context
import androidx.paging.DataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider

class NewsDataSourceFactory(
    private val appContext: Context
) : DataSource.Factory<Int, NewsItem>() {

    lateinit var category: String
    lateinit var language: String

    override fun create(): DataSource<Int, NewsItem> {
        val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
        return DailyHuntNewsRemoteDataSource(appContext, dailyHuntProvider, category, language)
        // TODO: support other sources
//        val newsProvider = NewsProvider.getNewsProvider()
//        return if (newsProvider?.isNewsPoint() == true) {
//            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
//            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
//                NewsRepository(DailyHuntNewsRemoteDataSource(appContext, dailyHuntProvider))
//            } else {
//                NewsRepository(NewsPointNewsRemoteDataSource(newsProvider))
//            }
//        } else {
//            NewsRepository(RssNewsRemoteDataSource(newsProvider))
//        }
    }
}