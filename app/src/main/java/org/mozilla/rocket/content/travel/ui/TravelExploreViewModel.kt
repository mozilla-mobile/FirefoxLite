package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.GetCityCategoriesUseCase
import org.mozilla.rocket.content.travel.domain.GetRunwayItemsUseCase

class TravelExploreViewModel(
    private val getRunwayItems: GetRunwayItemsUseCase,
    private val getCityCategories: GetCityCategoriesUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _items by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val data = ArrayList<DelegateAdapter.UiModel>()

                // addd search
                data.add(CitySearchUiModel())

                // add runway
                val runwayResult = getRunwayItems()
                if (runwayResult is Result.Success) {
                    data.add(TravelMapper.toRunway(runwayResult.data))
                }

                // add city category
                val cityCategoryResult = getCityCategories()
                if (cityCategoryResult is Result.Success) {
                    data.addAll(
                            cityCategoryResult.data.map {
                                TravelMapper.toCityCategoryUiModel(it)
                            }
                    )
                }

                // TODO: handle error

                value = data
            }
        }
    }
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _isDataLoading.value = State.Loading
                block()
                _isDataLoading.value = State.Idle
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}