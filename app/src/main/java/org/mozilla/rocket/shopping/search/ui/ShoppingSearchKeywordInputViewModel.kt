package org.mozilla.rocket.shopping.search.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import java.util.Locale

class ShoppingSearchKeywordInputViewModel(
    private val fetchKeywordSuggestion: FetchKeywordSuggestionUseCase
) : ViewModel() {

    private val _uiModel = MutableLiveData<ShoppingSearchKeywordInputUiModel>()
    val uiModel: LiveData<ShoppingSearchKeywordInputUiModel>
        get() = _uiModel

    fun fetchSuggestions(keyword: String) = viewModelScope.launch(Dispatchers.Default) {
        val fetchKeywordSuggestionResult = fetchKeywordSuggestion(keyword)
        if (fetchKeywordSuggestionResult is Result.Success) {
            val styledSuggestions = applyStyle(keyword, fetchKeywordSuggestionResult.data)
            withContext(Dispatchers.Main) {
                emitUiModel(styledSuggestions)
            }
        }
    }

    private fun applyStyle(keyword: String, keywordSuggestions: List<String>): List<CharSequence> {
        return keywordSuggestions.map { suggestion ->
            val idx = suggestion.toLowerCase(Locale.getDefault()).indexOf(keyword)
            if (idx != -1) {
                SpannableStringBuilder(suggestion).apply {
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx,
                        idx + keyword.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                suggestion
            }
        }
    }

    private fun emitUiModel(keywordSuggestions: List<CharSequence>) {
        _uiModel.value = ShoppingSearchKeywordInputUiModel(keywordSuggestions)
    }

    class Factory(
        val repository: KeywordSuggestionRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShoppingSearchKeywordInputViewModel::class.java)) {
                return ShoppingSearchKeywordInputViewModel(FetchKeywordSuggestionUseCase(repository)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }
}

data class ShoppingSearchKeywordInputUiModel(
    val keywordSuggestions: List<CharSequence>
)