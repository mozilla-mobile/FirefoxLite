package org.mozilla.rocket.content.news.data

class NewsRepositoryProvider(/*private val appContext: Context*/) {
    fun provideNewsRepository(): NewsRepository {
        return NewsRepository(NewsPointRemoteDataSource())
    }
}