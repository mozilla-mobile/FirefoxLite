package org.mozilla.rocket.content.travel.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase
import java.util.Locale

class TravelCitySearchViewModel(private val searchCityUseCase: SearchCityUseCase) : ViewModel() {

    private val _items = MutableLiveData<List<CitySearchResultUiModel>>()
    val items: LiveData<List<CitySearchResultUiModel>> = _items

    private var searchCityJob: Job? = null

    fun search(keyword: String) {
        if (searchCityJob?.isCompleted == false) {
            searchCityJob?.cancel()
        }

        searchCityJob = viewModelScope.launch {
            val result = searchCityUseCase(keyword)
            if (result is Result.Success) {
                _items.postValue(
                        result.data.map {
                            TravelMapper.toCitySearchResultUiModel(it.id, applyStyle(keyword, it.name))
                        }
                )
            }

            // TODO: handle error
        }
    }

    private fun applyStyle(keyword: String, keywordSerchResult: String): CharSequence {
        val idx = keywordSerchResult.toLowerCase(Locale.getDefault()).indexOf(keyword)
        if (idx != -1) {
            return SpannableStringBuilder(keywordSerchResult).apply {
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx,
                        idx + keyword.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            return keywordSerchResult
        }
    }
}