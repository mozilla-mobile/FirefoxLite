package org.mozilla.rocket.content.news.data

interface NewsMonitorDataSource {
    suspend fun trackItemsShown(items: List<NewsItem>)
}