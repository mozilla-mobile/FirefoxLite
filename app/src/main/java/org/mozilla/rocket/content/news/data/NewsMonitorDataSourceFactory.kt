package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntNewsMonitorDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.newspoint.NewsPointNewsMonitorDataSource
import org.mozilla.rocket.content.news.data.rss.RssNewsMonitorDataSource

class NewsMonitorDataSourceFactory(private val appContext: Context) {

    fun create(): NewsMonitorDataSource {
        val newsProvider = NewsProvider.getNewsProvider()
        return if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                DailyHuntNewsMonitorDataSource(dailyHuntProvider)
            } else {
                NewsPointNewsMonitorDataSource()
            }
        } else {
            RssNewsMonitorDataSource()
        }
    }
}