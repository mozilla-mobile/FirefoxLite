package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase

class NewsTabViewModel(private val loadNewsSettingsUseCase: LoadNewsSettingsUseCase) : ViewModel() {

    private val _uiModel = MutableLiveData<NewsTabUiModel>()
    val uiModel: LiveData<NewsTabUiModel>
        get() = _uiModel

    init {
        getNewsSettings()
    }

    fun getNewsSettings() = viewModelScope.launch(Dispatchers.Default) {
        val result = loadNewsSettingsUseCase()
        if (result is Result.Success) {
            withContext(Dispatchers.Main) { emitUiModel(result.data) }
        }
    }

    fun refresh() {
        _uiModel.value = NewsTabUiModel(Pair(LoadNewsLanguagesUseCase.DEFAULT_LANGUAGE, emptyList()))
        getNewsSettings()
    }

    private fun emitUiModel(newsSettings: Pair<NewsLanguage, List<NewsCategory>>) {
        _uiModel.value = NewsTabUiModel(newsSettings)
    }
}

// TODO update to hold the entire news elements
data class NewsTabUiModel(
    val newsSettings: Pair<NewsLanguage, List<NewsCategory>>
)