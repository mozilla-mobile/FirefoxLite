package org.mozilla.rocket.content.travel.ui

import android.content.Context
import org.mozilla.rocket.download.SingleLiveEvent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.utils.SearchUtils
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchGoogleUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultCategoryUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import java.util.Locale

class TravelCitySearchViewModel(private val searchCityUseCase: SearchCityUseCase) : ViewModel() {

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    private var searchCityJob: Job? = null
    val openCity = SingleLiveEvent<BaseCityData>()
    val changeClearBtnVisibility = SingleLiveEvent<Int>()
    val openGoogleSearch = SingleLiveEvent<String>()

    fun search(context: Context, keyword: String) {
        if (searchCityJob?.isCompleted == false) {
            searchCityJob?.cancel()
        }

        searchCityJob = viewModelScope.launch {
            val btnVisibility: Int
            val list = ArrayList<DelegateAdapter.UiModel>()

            if (keyword.isEmpty()) {
                btnVisibility = View.GONE
            } else {
                btnVisibility = View.VISIBLE
                val result = searchCityUseCase(keyword)
                if (result is Result.Success && !result.data.result.isEmpty()) {
                    list.add(CitySearchResultCategoryUiModel(R.drawable.ic_google, context.resources.getString(R.string.travel_search_engine_1, context.resources.getString(R.string.search_engine_name_google))))
                    list.add(CitySearchGoogleUiModel(keyword))
                    list.add(CitySearchResultCategoryUiModel(R.drawable.ic_search_black, context.resources.getString(R.string.travel_search_engine_fxlite, context.resources.getString(R.string.app_name))))
                    list.addAll(result.data.result.map {
                        TravelMapper.toCitySearchResultUiModel(it.id, applyStyle(keyword, it.name), it.country, it.type)
                    })
                }

                // TODO: handle error
            }
            _items.postValue(list)
            changeClearBtnVisibility.value = btnVisibility
        }
    }

    private fun applyStyle(keyword: String, keywordSerchResult: String): CharSequence {
        val idx = keywordSerchResult.toLowerCase(Locale.getDefault()).indexOf(keyword.toLowerCase(Locale.getDefault()))
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

    fun onCityClicked(it: CitySearchResultUiModel) {
        openCity.value = BaseCityData(it.id, it.name.toString(), it.type, "", "")
    }

    fun onGoogleSearchClicked(context: Context, keyword: String) {
        openGoogleSearch.value = SearchUtils.createSearchUrl(context, keyword)
    }
}