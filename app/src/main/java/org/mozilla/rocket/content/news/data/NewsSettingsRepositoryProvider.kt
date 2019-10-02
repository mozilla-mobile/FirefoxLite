package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.rss.RssSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.rss.RssSettingsRemoteDataSource

class NewsSettingsRepositoryProvider(private val appContext: Context) {
    fun provideNewsSettingsRepository(): NewsSettingsRepository {
        return NewsSettingsRepository(RssSettingsRemoteDataSource(), RssSettingsLocalDataSource(appContext))
    }
}