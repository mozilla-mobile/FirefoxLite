package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.newspoint.NewsPointNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssNewsRemoteDataSource

class NewsRepositoryProvider(private val appContext: Context) {

    fun provideNewsRepository(): NewsRepository {
        val newsProvider = NewsProvider.getNewsProvider()
        return if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                NewsRepository(DailyHuntNewsRemoteDataSource(appContext, dailyHuntProvider))
            } else {
                NewsRepository(NewsPointNewsRemoteDataSource(newsProvider))
            }
        } else {
            NewsRepository(RssNewsRemoteDataSource(newsProvider))
        }
    }
}