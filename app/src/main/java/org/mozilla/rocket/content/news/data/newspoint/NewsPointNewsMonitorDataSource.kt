package org.mozilla.rocket.content.news.data.newspoint

import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsMonitorDataSource

class NewsPointNewsMonitorDataSource : NewsMonitorDataSource {
    override suspend fun trackItemsShown(items: List<NewsItem>) = Unit
}