package org.mozilla.rocket.content.news.data

class NewsRepository(
    private val dataSourceFactory: NewsDataSourceFactory
) {

    fun getNewsItemsDataSourceFactory(): NewsDataSourceFactory = dataSourceFactory
}