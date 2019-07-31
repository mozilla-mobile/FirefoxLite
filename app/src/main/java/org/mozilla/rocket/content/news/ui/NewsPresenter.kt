package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.ui.NewsFragment.NewsListListener
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.threadutils.ThreadUtils

interface NewsViewContract {
    fun getViewLifecycleOwner(): LifecycleOwner
    fun updateNews(items: List<NewsItem>?)
    fun getCategory(): String
    fun getLanguage(): String
    fun updateSourcePriority()
}

class NewsPresenter(private val newsViewContract: NewsViewContract, private val newsViewModel: NewsViewModel) :
    NewsListListener {

    companion object {
        private val LOADMORE_THRESHOLD = 3000L
    }

    private var isLoading = false

    fun setupNewsViewModel(newsRepo: Repository<out NewsItem>, newsSettingRepo: NewsSettingsRepository) {

        val category = newsViewContract.getCategory()
        val newsLiveData: MediatorLiveData<List<NewsItem>>? =
            newsViewModel.getNews(category, newsViewContract.getLanguage(), newsRepo)

        val viewLifecycleOwner = newsViewContract.getViewLifecycleOwner()
        newsLiveData?.observe(
            viewLifecycleOwner,
            Observer { items ->
                newsViewContract.updateNews(items)
                isLoading = false
            })

        newsViewModel.newsSettingsRepository = newsSettingRepo
        newsViewContract.updateSourcePriority()
        newsViewModel.loadMore(category)
    }

    override fun loadMore() {
        if (!isLoading) {
            newsViewModel.loadMore(newsViewContract.getCategory())
            isLoading = true
            ThreadUtils.postToMainThreadDelayed(
                { isLoading = false },
                LOADMORE_THRESHOLD
            )
        }
    }
}
