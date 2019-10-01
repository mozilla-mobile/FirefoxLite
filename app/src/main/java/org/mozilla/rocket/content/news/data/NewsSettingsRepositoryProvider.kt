package org.mozilla.rocket.content.news.data

import android.content.Context

class NewsSettingsRepositoryProvider(private val appContext: Context) {
    fun provideNewsSettingsRepository(): NewsSettingsRepository {
        return NewsSettingsRepository(NewsPointSettingsRemoteDataSource(), NewsPointSettingsLocalDataSource(appContext))
    }
}