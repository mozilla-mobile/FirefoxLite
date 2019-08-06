package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase

class NewsViewModel(private val loadNewsSettingsUseCase: LoadNewsSettingsUseCase) : ViewModel() {

    private val _uiModel = MutableLiveData<NewsUiModel>()
    val uiModel: LiveData<NewsUiModel>
        get() = _uiModel

    private val newsMap = HashMap<String, MediatorLiveData<List<NewsItem>>>()

    private val useCaseMap = HashMap<String, LoadNewsUseCase>()

    lateinit var newsSettingsRepository: NewsSettingsRepository

    init {
        getNewsSettings()
    }

    private fun getNewsSettings() = viewModelScope.launch(Dispatchers.Default) {
        val result = loadNewsSettingsUseCase()
        if (result is Result.Success) {
            withContext(Dispatchers.Main) { emitUiModel(result.data) }
        }
    }

    private fun emitUiModel(newsSettings: Pair<NewsLanguage, List<NewsCategory>>) {
        _uiModel.value = NewsUiModel(newsSettings)
    }

    fun clear() {
        newsMap.clear()
        useCaseMap.clear()
    }

    fun getNews(category: String, lang: String, repo: Repository<out NewsItem>): MediatorLiveData<List<NewsItem>> {
        val items = newsMap[category] ?: MediatorLiveData()
        if (newsMap[category] == null) {
            newsMap[category] = items
            HashMap<String, String>().apply {
                this["category"] = category
                this["lang"] = lang
            }
            val loadNewsCase = LoadNewsUseCase(repo)
            useCaseMap[category] = loadNewsCase
            items.addSource(loadNewsCase.observe()) { result ->
                (result as? Result.Success)?.data?.let {
                    items.value = it.items
                }
            }
        }
        return items
    }

    fun loadMore(category: String) {
        useCaseMap[category]?.execute(LoadNewsParameter(category))
    }
}

// TODO update to hold the entire news elements
data class NewsUiModel(
    val newsSettings: Pair<NewsLanguage, List<NewsCategory>>
)