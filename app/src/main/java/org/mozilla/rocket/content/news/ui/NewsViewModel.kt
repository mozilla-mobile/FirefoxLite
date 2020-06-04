package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.Impression
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.TrackNewsItemsShownUseCase
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class NewsViewModel(
    private val loadNews: LoadNewsUseCase,
    private val trackNewsItemsShown: TrackNewsItemsShownUseCase
) : ViewModel() {

    private val categoryNewsMap = HashMap<String, LiveData<PagedList<DelegateAdapter.UiModel>>>()
    private val categoryParameterMap = HashMap<String, LoadNewsParameter>()
    private val categoryItemsShownMap = HashMap<String, Int>()
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
        categoryItemsShownMap.clear()
    }

    fun remove(category: String) {
        categoryNewsMap.remove(category)
        categoryParameterMap.remove(category)
        categoryItemsShownMap.remove(category)
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

    fun onNewsItemsShown(impression: Impression) = viewModelScope.launch(Dispatchers.Default) {
        val itemsLiveData = categoryNewsMap[impression.category]
        itemsLiveData?.value?.let {
            val newPosition = impression.positionMap[NewsItem.DEFAULT_SUB_CATEGORY_ID] ?: 0
            val lastPosition = categoryItemsShownMap[impression.category] ?: 0
            if (newPosition != 0 && newPosition > lastPosition && (((newPosition - lastPosition) > DEFAULT_PAGE_SIZE) || impression.significant)) {
                trackNewsItemsShown(it.subList(lastPosition, newPosition))
                categoryItemsShownMap[impression.category] = newPosition
            }
        }
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
