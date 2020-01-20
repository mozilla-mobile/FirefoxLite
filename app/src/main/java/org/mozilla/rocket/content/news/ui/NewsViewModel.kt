package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.domain.GetAdditionalSourceInfoUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.nextPage
import org.mozilla.rocket.content.news.ui.adapter.NewsSourceLogoUiModel
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class NewsViewModel(
    private val loadNews: LoadNewsUseCase,
    private val getAdditionalSourceInfo: GetAdditionalSourceInfoUseCase
) : ViewModel() {

    private val categoryNewsMap = HashMap<String, MutableLiveData<List<DelegateAdapter.UiModel>>>()
    private val categoryParameterMap = HashMap<String, LoadNewsParameter>()
    val versionId: Long = System.currentTimeMillis()

    val event = SingleLiveEvent<NewsAction>()

    fun startToObserveNews(category: String, language: String): LiveData<List<DelegateAdapter.UiModel>> {
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

    fun retry(category: String, language: String) {
        initialize(category, language)
    }

    fun onNewsItemClicked(category: String, newsItem: NewsUiModel) {
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
                if (loadNewsParameter.pages == 1) {
                    emitUiModel(
                        loadNewsParameter.topic,
                        getAdditionalSourceInfo()?.let { NewsSourceLogoUiModel(it.resourceId) },
                        newsItems
                    )
                } else {
                    emitUiModel(loadNewsParameter.topic, null, newsItems)
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                emitUiModel(loadNewsParameter.topic, null, emptyList())
            }
        }
    }

    private fun emitUiModel(category: String, newsSourceLogoUiModel: NewsSourceLogoUiModel?, newsItems: List<NewsItem>) {
        val newsData = categoryNewsMap[category] ?: return
        val results = arrayListOf<DelegateAdapter.UiModel>()
        newsData.value?.let {
            results.addAll(it)
        }

        newsSourceLogoUiModel?.let {
            results.add(it)
        }
        results.addAll(newsItems.map { NewsMapper.toNewsUiModel(it) })
        newsData.value = results
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 30
    }

    sealed class NewsAction {
        data class OpenLink(val url: String, val telemetryData: ContentTabTelemetryData) : NewsAction()
    }
}