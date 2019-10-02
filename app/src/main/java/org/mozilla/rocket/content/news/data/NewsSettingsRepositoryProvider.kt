package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.newspoint.NewsPointSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.newspoint.NewsPointSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.rss.RssSettingsRemoteDataSource

class NewsSettingsRepositoryProvider(private val appContext: Context) {

    fun provideNewsSettingsRepository(): NewsSettingsRepository {
        if (repository == null) {
            val newsProvider = NewsProvider.getNewsProvider()
            repository = if (newsProvider?.isNewsPoint() == true) {
                NewsSettingsRepository(NewsPointSettingsRemoteDataSource(newsProvider), NewsPointSettingsLocalDataSource(appContext))
            } else {
                NewsSettingsRepository(RssSettingsRemoteDataSource(newsProvider), RssSettingsLocalDataSource(appContext))
            }
        }

        return requireNotNull(repository)
    }

    companion object {
        var repository: NewsSettingsRepository? = null
    }
}