package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.news.data.rss.RssNewsRemoteDataSource

class NewsRepositoryProvider {
    fun provideNewsRepository(): NewsRepository {
        return NewsRepository(RssNewsRemoteDataSource())
    }
}