package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.AddToBucketListUseCase
import org.mozilla.rocket.content.travel.domain.CheckIsInBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetCityHotelsUseCase
import org.mozilla.rocket.content.travel.domain.GetCityIgUseCase
import org.mozilla.rocket.content.travel.domain.GetCityVideosUseCase
import org.mozilla.rocket.content.travel.domain.GetCityWikiUseCase
import org.mozilla.rocket.content.travel.domain.SetOnboardingHasShownUseCase
import org.mozilla.rocket.content.travel.domain.ShouldShowOnboardingUseCase
import org.mozilla.rocket.content.travel.domain.RemoveFromBucketListUseCase
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class TravelCityViewModel(
    private val getIg: GetCityIgUseCase,
    private val getWiki: GetCityWikiUseCase,
    private val getVideos: GetCityVideosUseCase,
    private val getHotels: GetCityHotelsUseCase,
    private val checkIsInBucketLis: CheckIsInBucketListUseCase,
    private val addToBucketList: AddToBucketListUseCase,
    private val removeFromBucketList: RemoveFromBucketListUseCase,
    private val shouldShowOnboarding: ShouldShowOnboardingUseCase,
    private val setOnboardingHasShown: SetOnboardingHasShownUseCase
) : ViewModel() {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _isInBucketList = MutableLiveData<Boolean>()
    val isInBucketList: LiveData<Boolean> = _isInBucketList

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    val showOnboardingSpotlight = SingleLiveEvent<Unit>()
    val showSnackBar = SingleLiveEvent<Unit>()
    val openLinkUrl = SingleLiveEvent<String>()

    init {
        if (shouldShowOnboarding()) {
            showOnboardingSpotlight.call()
            setOnboardingHasShown()
        }
    }

    fun checkIsInBucketList(id: String) {
        launchDataLoad {
            _isInBucketList.postValue(checkIsInBucketLis(id))
        }
    }

    fun getLatestItems(name: String) {
        data.clear()
        launchDataLoad {
            // TODO: add price items

            // add explore
            data.add(SectionHeaderUiModel(SectionType.Explore(name)))

            // TODO: uncomment these while corresponding adapter delegates is added
            val igResult = getIg(name)
            if (igResult is Result.Success) {
                data.add(TravelMapper.toExploreIgUiModel(igResult.data))
            }

            val videoResult = getVideos(name)
            if (videoResult is Result.Success) {
                data.addAll(
                        videoResult.data.map {
                            // TODO: handle real read stats
                            TravelMapper.toVideoUiModel(it, false)
                        }
                )
            }

            val wikiResult = getWiki(name)
            if (wikiResult is Result.Success) {
                data.add(TravelMapper.toExploreWikiUiModel(wikiResult.data))
            }

            // add hotel
            data.add(SectionHeaderUiModel(SectionType.TopHotels))

            val hotelResult = getHotels(name)
            if (hotelResult is Result.Success) {
                data.addAll(
                        hotelResult.data.map {
                            TravelMapper.toHotelUiModel(it)
                        }
                )
            }

            // TODO: handle error

            _items.postValue(data)
        }
    }

    fun onFavoriteToggled(city: BaseCityData, isSelected: Boolean) {
        viewModelScope.launch {
            if (isSelected) {
                removeFromBucketList(city.id)
            } else {
                addToBucketList(city.id, city.name)
                showSnackBar.call()
            }

            _isInBucketList.postValue(!isSelected)
        }
    }

    fun onIgClicked(igItem: IgUiModel) {
        openLinkUrl.value = igItem.linkUrl
    }

    @Suppress("UNUSED_PARAMETER")
    fun onVideoClicked(videoItem: VideoUiModel) {
        // TODO handle video click
    }

    fun onWikiClicked(wikiItem: WikiUiModel) {
        openLinkUrl.value = wikiItem.linkUrl
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

    sealed class SectionType {
        data class Explore(val name: String) : SectionType()
        object TopHotels : SectionType()
    }
}