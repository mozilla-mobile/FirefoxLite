package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.nextPage
import org.mozilla.rocket.download.SingleLiveEvent

class NewsViewModel(private val loadNews: LoadNewsUseCase) : ViewModel() {

    private val categoryNewsMap = HashMap<String, MutableLiveData<NewsUiModel>>()
    private val categoryParameterMap = HashMap<String, LoadNewsParameter>()
    private val versionId: Long = System.currentTimeMillis()

    val event = SingleLiveEvent<NewsAction>()

    fun startToObserveNews(category: String, language: String): LiveData<NewsUiModel> {
        if (categoryNewsMap[category] == null) {
            categoryNewsMap[category] = MutableLiveData()
            initialize(category, language)
        }

        return requireNotNull(categoryNewsMap[category])
    }

    fun loadMore(category: String) = viewModelScope.launch(Dispatchers.Default) {
        val nextPageNewsParameter =
            requireNotNull(categoryParameterMap[category]) { "Need to call 'startToObserveNews' with category: $category before 'loadMore'" }
                .nextPage()

        categoryParameterMap[category] = nextPageNewsParameter
        updateNews(nextPageNewsParameter)
    }

    fun clear() {
        categoryNewsMap.clear()
        categoryParameterMap.clear()
    }

    fun onNewsItemClicked(category: String, newsItem: NewsItem) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.LIFESTYLE,
            newsItem.feed,
            newsItem.source,
            category,
            newsItem.componentId,
            newsItem.subCategoryId,
            versionId
        )
        event.value = NewsAction.OpenLink(newsItem.link, telemetryData)
    }

    private fun initialize(category: String, language: String) = viewModelScope.launch(Dispatchers.Default) {
        val loadNewsParameter = LoadNewsParameter(category, language, 1, DEFAULT_PAGE_SIZE)
        categoryParameterMap[category] = loadNewsParameter
        updateNews(loadNewsParameter)
    }

    private suspend fun updateNews(loadNewsParameter: LoadNewsParameter) {
        val newsResults = loadNews(loadNewsParameter)
        if (newsResults is Result.Success) {
            val newsItems = newsResults.data
            withContext(Dispatchers.Main) {
                emitUiModel(loadNewsParameter.topic, newsItems)
            }
        } else {
            withContext(Dispatchers.Main) {
                emitUiModel(loadNewsParameter.topic, emptyList())
            }
        }
    }

    private fun emitUiModel(category: String, newsItems: List<NewsItem>) {
        val newsData = categoryNewsMap[category] ?: return
        val results = arrayListOf<NewsItem>()
        newsData.value?.newsList?.let {
            results.addAll(it)
        }
        results.addAll(newsItems)
        newsData.value = NewsUiModel(results)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 30
    }

    sealed class NewsAction {
        data class OpenLink(val url: String, val telemetryData: ContentTabTelemetryData) : NewsAction()
    }

    data class NewsUiModel(
        val newsList: List<NewsItem>
    )
}