package org.mozilla.rocket.content.news.data

class NewsRepository(
    private val dataSourceFactory: NewsDataSourceFactory,
    private val monitorDataSourceFactory: NewsMonitorDataSourceFactory
) {

    fun getNewsItemsDataSourceFactory(): NewsDataSourceFactory = dataSourceFactory

    fun getNewsMonitorDataSourceFactory(): NewsMonitorDataSourceFactory = monitorDataSourceFactory
}