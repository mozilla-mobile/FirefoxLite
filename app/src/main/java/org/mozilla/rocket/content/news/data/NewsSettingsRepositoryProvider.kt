package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.newspoint.NewsPointSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.newspoint.NewsPointSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.rss.RssSettingsRemoteDataSource

class NewsSettingsRepositoryProvider(private val appContext: Context) {

    fun provideNewsSettingsRepository(): NewsSettingsRepository {
        val newsProvider = NewsProvider.getNewsProvider()
        return if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                NewsSettingsRepository(DailyHuntSettingsRemoteDataSource(dailyHuntProvider), DailyHuntSettingsLocalDataSource(appContext))
            } else {
                NewsSettingsRepository(NewsPointSettingsRemoteDataSource(newsProvider, dailyHuntProvider), NewsPointSettingsLocalDataSource(appContext))
            }
        } else {
            NewsSettingsRepository(RssSettingsRemoteDataSource(newsProvider), RssSettingsLocalDataSource(appContext))
        }
    }
}