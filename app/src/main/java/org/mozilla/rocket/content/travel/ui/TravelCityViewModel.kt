package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
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
import org.mozilla.rocket.content.travel.ui.adapter.HotelUiModel
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
    private var dataLoadingCount = 0
    private var isHotelLoading = false

    private val _isInBucketList = MutableLiveData<Boolean>()
    val isInBucketList: LiveData<Boolean> = _isInBucketList

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    val showOnboardingSpotlight = SingleLiveEvent<Unit>()
    val showSnackBar = SingleLiveEvent<Unit>()
    val openLinkUrl = SingleLiveEvent<String>()

    private var hotelsCount = 0
    private lateinit var id: String
    private lateinit var type: String

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

    fun getLatestItems(context: Context, name: String, id: String, type: String) {
        data.clear()
        hotelsCount = 0
        this.id = id
        this.type = type
        launchDataLoad {
            // TODO: add price items

            // add explore
            data.add(SectionHeaderUiModel(SectionType.Explore(name)))

            val igResult = getIg(name)
            if (igResult is Result.Success) {
                data.add(TravelMapper.toExploreIgUiModel(igResult.data))
            }

            val videoResult = getVideos(String.format(VIDEO_QUERY_PATTERN, Uri.encode(name), context.resources.getString(R.string.travel_vertical_title)))
            if (videoResult is Result.Success) {
                data.addAll(
                        videoResult.data.videos.map {
                            // TODO: handle real read stats
                            TravelMapper.toVideoUiModel(it, false)
                        }
                )
            }

            val wikiResult = getWiki(Uri.encode(name))
            if (wikiResult is Result.Success) {
                data.add(TravelMapper.toExploreWikiUiModel(wikiResult.data, context.resources.getString(R.string.travel_content_wiki_source_name)))
            }

            // add hotel
            data.add(SectionHeaderUiModel(SectionType.TopHotels))

            val hotelResult = getHotels(id, type, hotelsCount)
            if (hotelResult is Result.Success) {
                data.addAll(
                        hotelResult.data.result.map {
                            TravelMapper.toHotelUiModel(it)
                        }
                )
                hotelsCount = hotelResult.data.result.size
            }
            // TODO: handle error

            _items.postValue(data)
        }
    }

    private fun loadMoreHotels() {
        if (!isHotelLoading) {

            isHotelLoading = true
            backgroundHotelLoad {
                // add more hotels
                val hotelResult = getHotels(id, type, hotelsCount)
                if (hotelResult is Result.Success) {
                    data.addAll(
                            hotelResult.data.result.map {
                                TravelMapper.toHotelUiModel(it)
                            }
                    )
                    hotelsCount += hotelResult.data.result.size
                    _items.postValue(data)
                }

                isHotelLoading = false
            }
        }
    }

    fun onFavoriteToggled(city: BaseCityData, isSelected: Boolean) {
        viewModelScope.launch {
            if (isSelected) {
                removeFromBucketList(city.id)
            } else {
                addToBucketList(city.id, city.name, city.type, city.nameInEnglish, city.countryCode)
                showSnackBar.call()
            }

            _isInBucketList.postValue(!isSelected)
        }
    }

    fun onIgClicked(igItem: IgUiModel) {
        openLinkUrl.value = igItem.linkUrl
    }

    fun onVideoClicked(videoItem: VideoUiModel) {
        openLinkUrl.value = videoItem.linkUrl
    }

    fun onWikiClicked(wikiItem: WikiUiModel) {
        openLinkUrl.value = wikiItem.linkUrl
    }

    fun onHotelClicked(hotelItem: HotelUiModel) {
        openLinkUrl.value = hotelItem.linkUrl
    }

    fun onDetailItemScrolled(firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int, isScrollDown: Boolean) {
        if (isScrollDown && firstVisibleItem + visibleItemCount + LOAD_MORE_HOTELS_THRESHOLD >= totalItemCount) {
            loadMoreHotels()
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                setDataLoadingState(State.Loading)
                block()
                setDataLoadingState(State.Idle)
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    private fun backgroundHotelLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    private fun setDataLoadingState(state: State) {
        when (state) {
            is State.Idle -> {
                if (dataLoadingCount > 0) {
                    dataLoadingCount--
                }
            }
            is State.Loading -> {
                dataLoadingCount++
            }
        }

        if (dataLoadingCount == 0) {
            _isDataLoading.value = State.Idle
        } else {
            _isDataLoading.value = State.Loading
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

    companion object {
        private const val LOAD_MORE_HOTELS_THRESHOLD = 15
        private const val VIDEO_QUERY_PATTERN = "%s+%s"
    }
}