package org.mozilla.rocket.content.news.data

import android.content.Context
import androidx.paging.DataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.content.news.data.newspoint.NewsPointNewsRemoteDataSource
import org.mozilla.rocket.content.news.data.rss.RssNewsRemoteDataSource
import org.mozilla.rocket.content.news.domain.GetAdditionalSourceInfoUseCase

class NewsDataSourceFactory(
    private val appContext: Context,
    private val getAdditionalSourceInfo: GetAdditionalSourceInfoUseCase
) : DataSource.Factory<NewsDataSourceFactory.PageKey, NewsItem>() {

    lateinit var category: String
    lateinit var language: String

    override fun create(): DataSource<PageKey, NewsItem> {
        val newsProvider = NewsProvider.getNewsProvider()
        return if (newsProvider?.isNewsPoint() == true) {
            val dailyHuntProvider = DailyHuntProvider.getProvider(appContext)
            if (dailyHuntProvider?.shouldEnable(appContext) == true) {
                DailyHuntNewsRemoteDataSource(appContext, getAdditionalSourceInfo, dailyHuntProvider, category, language)
            } else {
                NewsPointNewsRemoteDataSource(newsProvider, category, language)
            }
        } else {
            RssNewsRemoteDataSource(newsProvider, category)
        }
    }

    sealed class PageKey {
        class PageNumberKey(val number: Int) : PageKey()
        class PageUrlKey(val url: String) : PageKey()
    }
}