package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.news.data.newspoint.NewsPointNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssNewsRemoteDataSource

class NewsRepositoryProvider {

    fun provideNewsRepository(): NewsRepository {
        if (repository == null) {
            val newsProvider = NewsProvider.getNewsProvider()
            repository = if (newsProvider?.isNewsPoint() == true) {
                NewsRepository(NewsPointNewsRemoteDataSource(newsProvider))
            } else {
                NewsRepository(RssNewsRemoteDataSource(newsProvider))
            }
        }

        return requireNotNull(repository)
    }

    companion object {
        var repository: NewsRepository? = null
    }
}