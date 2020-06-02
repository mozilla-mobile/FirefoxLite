package org.mozilla.rocket.content.news.data.rss

import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsMonitorDataSource

class RssNewsMonitorDataSource : NewsMonitorDataSource {
    override suspend fun trackItemsShown(items: List<NewsItem>) = Unit
}