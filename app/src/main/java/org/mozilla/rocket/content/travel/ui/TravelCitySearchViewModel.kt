package org.mozilla.rocket.content.travel.ui

import android.graphics.Color
import org.mozilla.rocket.download.SingleLiveEvent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import java.util.Locale

class TravelCitySearchViewModel(private val searchCityUseCase: SearchCityUseCase) : ViewModel() {

    private val _items = MutableLiveData<List<CitySearchResultUiModel>>()
    val items: LiveData<List<CitySearchResultUiModel>> = _items

    private var searchCityJob: Job? = null
    val openCity = SingleLiveEvent<BaseCityData>()
    val changeClearBtnVisibility = SingleLiveEvent<Int>()

    fun search(keyword: String) {
        if (searchCityJob?.isCompleted == false) {
            searchCityJob?.cancel()
        }

        searchCityJob = viewModelScope.launch {
            val btnVisibility: Int
            var list: List<CitySearchResultUiModel> = emptyList()

            if (keyword.isEmpty()) {
                btnVisibility = View.GONE
            } else {
                btnVisibility = View.VISIBLE
                val result = searchCityUseCase(keyword)
                if (result is Result.Success) {
                    list = result.data.map {
                        TravelMapper.toCitySearchResultUiModel(it.id, applyStyle(keyword, it.name))
                    }
                }

                // TODO: handle error
            }
            _items.postValue(list)
            changeClearBtnVisibility.value = btnVisibility
        }
    }

    private fun applyStyle(keyword: String, keywordSerchResult: String): CharSequence {
        val idx = keywordSerchResult.toLowerCase(Locale.getDefault()).indexOf(keyword.toLowerCase())
        if (idx != -1) {
            return SpannableStringBuilder(keywordSerchResult).apply {
                setSpan(ForegroundColorSpan(Color.BLACK),
                    idx,
                    idx + keyword.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(TypefaceSpan("sans-serif-medium"),
                    idx,
                    idx + keyword.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        } else {
            return keywordSerchResult
        }
    }

    fun onCityClicked(it: CitySearchResultUiModel) {
        openCity.value = BaseCityData(it.id, it.name.toString())
    }
}