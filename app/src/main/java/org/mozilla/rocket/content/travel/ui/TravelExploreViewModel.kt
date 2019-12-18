package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.GetExploreListUseCase
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class TravelExploreViewModel(private val getExploreList: GetExploreListUseCase) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _exploreItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val exploreItems: LiveData<List<DelegateAdapter.UiModel>> = _exploreItems

    var versionId = 0L

    val openCity = SingleLiveEvent<BaseCityData>()
    val goSearch = SingleLiveEvent<Unit>()

    fun onCityItemClicked(cityItem: CityUiModel) {
        openCity.value = BaseCityData(cityItem.id, cityItem.name, cityItem.type, cityItem.nameInEnglish, cityItem.countryCode)
        TelemetryWrapper.clickContentHomeItem(TelemetryWrapper.Extra_Value.TRAVEL, TelemetryWrapper.Extra_Value.EXPLORE, cityItem.id, cityItem.getTelemetryItemName())
    }

    fun onSearchInputClicked() {
        goSearch.call()
        TelemetryWrapper.showContentHomeSearchBar(TelemetryWrapper.Extra_Value.TRAVEL)
    }

    fun requestExploreList() {
        getExploreUiModelList()
    }

    private fun getExploreUiModelList() {
        launchDataLoad {
            val data = ArrayList<DelegateAdapter.UiModel>()

            // add search
            data.add(CitySearchUiModel())

            // add explore list
            val exploreListResult = getExploreList()
            if (exploreListResult is Result.Success) {
                versionId = exploreListResult.data.version
                data.addAll(
                    TravelMapper.toExploreList(exploreListResult.data)
                )
            }

            // TODO: handle error
            _exploreItems.postValue(data)
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
