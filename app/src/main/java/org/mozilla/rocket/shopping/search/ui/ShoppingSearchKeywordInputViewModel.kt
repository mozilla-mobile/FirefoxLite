package org.mozilla.rocket.shopping.search.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.CheckOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CompleteOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import java.util.Locale

class ShoppingSearchKeywordInputViewModel(
    private val fetchKeywordSuggestion: FetchKeywordSuggestionUseCase,
    checkOnboardingFirstRunUseCase: CheckOnboardingFirstRunUseCase,
    private val completeOnboardingFirstRunUseCase: CompleteOnboardingFirstRunUseCase
) : ViewModel() {

    private val _uiModel = MutableLiveData<ShoppingSearchKeywordInputUiModel>()
    val uiModel: LiveData<ShoppingSearchKeywordInputUiModel>
        get() = _uiModel

    val navigateToResultTab = SingleLiveEvent<String>()
    private var isFirstRun = false

    init {
        isFirstRun = checkOnboardingFirstRunUseCase()
        emitUiModel(ShoppingSearchKeywordInputUiModel(hideClear = true, hideHintContainer = !isFirstRun))
    }

    fun fetchSuggestions(keyword: String) = viewModelScope.launch(Dispatchers.Default) {
        val newUiModel: ShoppingSearchKeywordInputUiModel
        if (TextUtils.isEmpty(keyword)) {
            newUiModel = ShoppingSearchKeywordInputUiModel(hideClear = true, hideHintContainer = !isFirstRun)
        } else {
            var styledSuggestions: List<CharSequence>? = null
            val fetchKeywordSuggestionResult = fetchKeywordSuggestion(keyword)
            if (fetchKeywordSuggestionResult is Result.Success) {
                styledSuggestions = applyStyle(keyword, fetchKeywordSuggestionResult.data)
            }
            newUiModel = ShoppingSearchKeywordInputUiModel(styledSuggestions, true, true, true)
        }

        withContext(Dispatchers.Main) {
            emitUiModel(newUiModel)
        }
    }

    fun onKeywordSent(keyword: String) {
        if (!TextUtils.isEmpty(keyword)) {
            navigateToResultTab.value = keyword
            if (isFirstRun) {
                completeOnboardingFirstRunUseCase()
                isFirstRun = false
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

    private fun emitUiModel(newUiModel: ShoppingSearchKeywordInputUiModel) {
        _uiModel.value = newUiModel
    }
}

data class ShoppingSearchKeywordInputUiModel(
    val keywordSuggestions: List<CharSequence>? = null,
    var hideHintContainer: Boolean = false,
    val hideLogoMan: Boolean = false,
    val hideIndication: Boolean = false,
    val hideClear: Boolean = false
)