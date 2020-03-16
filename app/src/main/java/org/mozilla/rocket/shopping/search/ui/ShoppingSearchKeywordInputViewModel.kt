package org.mozilla.rocket.shopping.search.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import java.util.Locale

class ShoppingSearchKeywordInputViewModel(private val fetchKeywordSuggestion: FetchKeywordSuggestionUseCase) : ViewModel() {

    private val _uiModel = MutableLiveData<ShoppingSearchKeywordInputUiModel>()
    val uiModel: LiveData<ShoppingSearchKeywordInputUiModel>
        get() = _uiModel

    val navigateToResultTab = SingleLiveEvent<String>()
    private var fetchSuggestionsJob: Job? = null
    private var firstTimeTyping: Boolean = true

    fun onStart() {
        TelemetryWrapper.showSearchBarFromTabSwipe(TelemetryWrapper.Extra_Value.SHOPPING)
    }

    fun onKeyboardShown() {
        TelemetryWrapper.showKeyboardFromTabSwipeSearchBar(TelemetryWrapper.Extra_Value.SHOPPING)
    }

    fun onTypingKeyword(keyword: String) {
        if (fetchSuggestionsJob?.isCompleted == false) {
            fetchSuggestionsJob?.cancel()
        }

        fetchSuggestionsJob = viewModelScope.launch(Dispatchers.Default) {
            val newUiModel: ShoppingSearchKeywordInputUiModel
            if (TextUtils.isEmpty(keyword)) {
                newUiModel = ShoppingSearchKeywordInputUiModel(hideClear = true)
            } else {
                var styledSuggestions: List<CharSequence>? = null
                val fetchKeywordSuggestionResult = fetchKeywordSuggestion(keyword)
                if (fetchKeywordSuggestionResult is Result.Success) {
                    styledSuggestions = applyStyle(keyword, fetchKeywordSuggestionResult.data)
                }
                newUiModel = ShoppingSearchKeywordInputUiModel(styledSuggestions, hideClear = false)
            }

            withContext(Dispatchers.Main) {
                emitUiModel(newUiModel)
            }
        }

        if (firstTimeTyping) {
            TelemetryWrapper.startTypingFromTabSwipeSearchBar(TelemetryWrapper.Extra_Value.SHOPPING)
            firstTimeTyping = false
        }
    }

    fun onTypedKeywordSent(keyword: String) {
        onKeywordSent(keyword)
        TelemetryWrapper.searchWithTextInSearchBar(TelemetryWrapper.Extra_Value.SHOPPING)
    }

    fun onSuggestionKeywordSent(keyword: String) {
        onKeywordSent(keyword)
        TelemetryWrapper.useSearchSuggestionInTabSwipeSearchBar(TelemetryWrapper.Extra_Value.SHOPPING, false, "null")
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

    private fun emitUiModel(newUiModel: ShoppingSearchKeywordInputUiModel) {
        _uiModel.value = newUiModel
    }

    private fun onKeywordSent(keyword: String) {
        if (!TextUtils.isEmpty(keyword)) {
            navigateToResultTab.value = keyword
        }
    }
}

data class ShoppingSearchKeywordInputUiModel(
    val keywordSuggestions: List<CharSequence>? = null,
    val hideClear: Boolean = false
)