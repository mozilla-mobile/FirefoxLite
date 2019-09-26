package org.mozilla.rocket.content.news.data

class NewsRepositoryProvider(/*private val appContext: Context*/) {
    fun provideNewsRepository(): NewsRepositoryV2 {
        return NewsRepositoryV2(NewsPointRemoteDataSource())
    }
}