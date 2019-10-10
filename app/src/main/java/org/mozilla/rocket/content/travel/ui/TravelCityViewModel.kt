package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.domain.GetCityHotelsUseCase
import org.mozilla.rocket.content.travel.domain.GetCityIgUseCase
import org.mozilla.rocket.content.travel.domain.GetCityVideosUseCase
import org.mozilla.rocket.content.travel.domain.GetCityWikiUseCase
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import org.mozilla.rocket.download.SingleLiveEvent

class TravelCityViewModel(
    private val getIg: GetCityIgUseCase,
    private val getWiki: GetCityWikiUseCase,
    private val getVideos: GetCityVideosUseCase,
    private val getHotels: GetCityHotelsUseCase
) : ViewModel() {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    val openLinkUrl = SingleLiveEvent<String>()

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

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

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

            // TODO: uncomment these while corresponding adapter delegates is added
            /*
            val hotelResult = getHotels(name)
            if (hotelResult is Result.Success) {
                data.addAll(
                        hotelResult.data.map {
                            TravelMapper.toHotelUiModel(it)
                        }
                )
            }
            */

            // TODO: handle error

            _items.postValue(data)
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

    sealed class SectionType {
        data class Explore(val name: String) : SectionType()
        object TopHotels : SectionType()
    }
}