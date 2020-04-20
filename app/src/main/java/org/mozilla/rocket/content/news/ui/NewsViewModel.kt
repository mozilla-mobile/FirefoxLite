package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class NewsViewModel(
    private val loadNews: LoadNewsUseCase
) : ViewModel() {

    private val categoryNewsMap = HashMap<String, LiveData<PagedList<DelegateAdapter.UiModel>>>()
    private val categoryParameterMap = HashMap<String, LoadNewsParameter>()
    val versionId: Long = System.currentTimeMillis()

    val event = SingleLiveEvent<NewsAction>()

    fun startToObserveNews(category: String, language: String): LiveData<PagedList<DelegateAdapter.UiModel>> {
        if (categoryNewsMap[category] == null) {
            initialize(category, language)
        }

        return requireNotNull(categoryNewsMap[category])
    }

    fun clear() {
        categoryNewsMap.clear()
        categoryParameterMap.clear()
    }

    fun remove(category: String) {
        categoryNewsMap.remove(category)
        categoryParameterMap.remove(category)
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

    private fun initialize(category: String, language: String) {
        val loadNewsParameter = LoadNewsParameter(category, language, DEFAULT_PAGE_SIZE)
        categoryParameterMap[category] = loadNewsParameter
        categoryNewsMap[category] = loadNews(loadNewsParameter)
        // TODO: Evan: add source icon item back
        // getAdditionalSourceInfo()?.let { NewsSourceLogoUiModel(it.resourceId) }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 30
    }

    sealed class NewsAction {
        data class OpenLink(val url: String, val telemetryData: ContentTabTelemetryData) : NewsAction()
    }
}