package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.GetBucketListUseCase
import org.mozilla.rocket.content.travel.domain.RemoveFromBucketListUseCase
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class TravelBucketListViewModel(
    private val getBucketListUseCase: GetBucketListUseCase,
    private val removeFromBucketListUseCase: RemoveFromBucketListUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    val openCity = SingleLiveEvent<BaseCityData>()
    val goSearch = SingleLiveEvent<Unit>()

    fun getBucketList() {
        launchDataLoad {
            loadBucketList()
        }
    }

    fun onBucketListCityClicked(cityItem: BucketListCityUiModel) {
        openCity.value = BaseCityData(cityItem.id, cityItem.name)
    }

    fun onExploreCityClicked() {
        goSearch.call()
    }

    fun removeCityFromBucket(cityItem: BucketListCityUiModel) {
        launchDataLoad {
            removeFromBucketListUseCase(cityItem.id)
            loadBucketList()
        }
    }

    private suspend fun loadBucketList() {
        val result = getBucketListUseCase()
        if (result is Result.Success) {
            _items.postValue(result.data.map { TravelMapper.toBucketListCityUiModel(it) })
        } else if (result is Result.Error) {
            throw (result.exception)
        }
    }

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