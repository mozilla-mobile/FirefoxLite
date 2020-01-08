package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.travel.domain.AddToBucketListUseCase
import org.mozilla.rocket.content.travel.domain.CheckIsInBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetCityHotelsUseCase
import org.mozilla.rocket.content.travel.domain.GetCityIgUseCase
import org.mozilla.rocket.content.travel.domain.GetCityVideosUseCase
import org.mozilla.rocket.content.travel.domain.GetCityWikiUseCase
import org.mozilla.rocket.content.travel.domain.GetEnglishNameUseCase
import org.mozilla.rocket.content.travel.domain.GetMoreHotelsUrlUseCase
import org.mozilla.rocket.content.travel.domain.RemoveFromBucketListUseCase
import org.mozilla.rocket.content.travel.domain.SetOnboardingHasShownUseCase
import org.mozilla.rocket.content.travel.domain.SetTravelDiscoveryAsDefaultUseCase
import org.mozilla.rocket.content.travel.domain.ShouldShowChangeTravelSearchSettingUseCase
import org.mozilla.rocket.content.travel.domain.ShouldShowOnboardingUseCase
import org.mozilla.rocket.content.travel.ui.adapter.HotelUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.LoadingUiModel
import org.mozilla.rocket.content.travel.ui.adapter.SectionHeaderUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.sha256

class TravelCityViewModel(
    private val getIg: GetCityIgUseCase,
    private val getWiki: GetCityWikiUseCase,
    private val getVideos: GetCityVideosUseCase,
    private val getHotels: GetCityHotelsUseCase,
    private val checkIsInBucketLis: CheckIsInBucketListUseCase,
    private val addToBucketList: AddToBucketListUseCase,
    private val removeFromBucketList: RemoveFromBucketListUseCase,
    private val getEnglishName: GetEnglishNameUseCase,
    private val getMoreHotelsUrl: GetMoreHotelsUrlUseCase,
    private val shouldShowOnboarding: ShouldShowOnboardingUseCase,
    private val setOnboardingHasShown: SetOnboardingHasShownUseCase,
    private val shouldShowChangeTravelSearchSetting: ShouldShowChangeTravelSearchSettingUseCase,
    private val setTravelDiscoveryAsDefault: SetTravelDiscoveryAsDefaultUseCase
) : ViewModel() {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading
    private var dataLoadingCount = 0
    private var isHotelLoading = false

    private val _isInBucketList = MutableLiveData<Boolean>()
    val isInBucketList: LiveData<Boolean> = _isInBucketList

    private val _englishCityName = MutableLiveData<String>()
    val englishCityName: LiveData<String> = _englishCityName

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    val showOnboardingSpotlight = SingleLiveEvent<Unit>()
    val showSnackBar = SingleLiveEvent<Unit>()
    val openLink = SingleLiveEvent<OpenLinkAction>()
    val showChangeSearchSettingPrompt = SingleLiveEvent<Unit>()
    val changeSearchSettingFinished = SingleLiveEvent<Unit>()

    private var loadingJob: Job? = null
    private var loadMoreJob: Job? = null
    private var hotelsCount = 0
    private lateinit var city: BaseCityData
    private val loadingUiModel = LoadingUiModel()
    private var shouldShowChangeSearchSettingPrompt = false
    private var doNotAskChangeSearchSettingAgain = false

    var category: String = ""
    val versionId: Long = System.currentTimeMillis()

    init {
        if (shouldShowOnboarding()) {
            showOnboardingSpotlight.call()
            setOnboardingHasShown()
        }

        shouldShowChangeSearchSettingPrompt = shouldShowChangeTravelSearchSetting()
    }

    fun checkIsInBucketList(id: String) {
        backgroundTask {
            _isInBucketList.postValue(checkIsInBucketLis(id))
        }
    }

    fun getLatestItems(context: Context, city: BaseCityData) {
        data.clear()
        dataLoadingCount = 0
        hotelsCount = 0
        isHotelLoading = false
        this.city = city

        loadMoreJob?.cancel()
        loadingJob?.cancel()
        loadingJob = launchDataLoad(
            {
                // TODO: add price items

                // add explore
                data.add(SectionHeaderUiModel(SectionType.Explore(city.name)))

                val englishNameResult = getEnglishName(city.id, city.type)
                val englishName = if (englishNameResult is Result.Success) {
                    _englishCityName.value = englishNameResult.data
                    englishNameResult.data
                } else {
                    city.name
                }
                this.city = city.copy(nameInEnglish = englishName)

                val igResult = getIg(englishName)
                if (igResult is Result.Success) {
                    data.add(TravelMapper.toExploreIgUiModel(igResult.data))
                }

                val videoResult = getVideos(String.format(VIDEO_QUERY_PATTERN, Uri.encode(city.name), context.resources.getString(R.string.travel_vertical_title)))
                if (videoResult is Result.Success) {
                    data.addAll(
                            videoResult.data.videos.map {
                                // TODO: handle real read stats
                                TravelMapper.toVideoUiModel(it, false)
                            }
                    )
                }

                val wikiResult = getWiki(city.name)
                if (wikiResult is Result.Success) {
                    data.add(TravelMapper.toExploreWikiUiModel(wikiResult.data, context.resources.getString(R.string.travel_content_wiki_source_name)))
                }

                // add hotel section header
                val moreHotelsUrlResult = getMoreHotelsUrl(city.name, city.id, city.type)
                val hotelHeader = if (moreHotelsUrlResult is Result.Success) {
                    SectionHeaderUiModel(SectionType.TopHotels, moreHotelsUrlResult.data)
                } else {
                    SectionHeaderUiModel(SectionType.TopHotels)
                }
                data.add(hotelHeader)
                yield()

                _items.postValue(data)
            }, {
                // add hotels
                loadHotels()
            }
        )
    }

    private suspend fun loadHotels() {
        data.add(loadingUiModel)
        _items.postValue(data)

        isHotelLoading = true
        val hotelResult = getHotels(city.id, city.type, hotelsCount)

        data.remove(loadingUiModel)
        yield()
        if (hotelResult is Result.Success) {
            hotelsCount += hotelResult.data.result.size
            data.addAll(
                hotelResult.data.result.filterNotNull()
                    .filter { it.price > 0f }
                    .map { TravelMapper.toHotelUiModel(it) }
            )
        }

        _items.postValue(data)
        isHotelLoading = false
    }

    private fun loadMoreHotels() {
        if (!isHotelLoading) {
            loadMoreJob = backgroundTask {
                loadHotels()
            }
        }
    }

    fun onBackPressed() {
        if (shouldShowChangeSearchSettingPrompt) {
            showChangeSearchSettingPrompt.call()
            TelemetryWrapper.showSetDefaultTravelSearchMessage()
        } else {
            changeSearchSettingFinished.call()
        }
    }

    fun onDoNotAskMeAgainAction(isSelected: Boolean) {
        doNotAskChangeSearchSettingAgain = isSelected
    }

    fun onChangeSearchSettingAction(setTravelDiscoveryAsDefault: Boolean) {
        viewModelScope.launch {
            changeSearchSettingFinished.call()
            if (setTravelDiscoveryAsDefault) {
                setTravelDiscoveryAsDefault(true)
                TelemetryWrapper.clickSetDefaultTravelSearchMessage(TelemetryWrapper.Extra_Value.SET_DEFAULT)
            } else {
                if (doNotAskChangeSearchSettingAgain) {
                    setTravelDiscoveryAsDefault(false)
                }
                TelemetryWrapper.clickSetDefaultTravelSearchMessage(TelemetryWrapper.Extra_Value.CLOSE)
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
        val action = if (isSelected) {
            TelemetryWrapper.Extra_Value.REMOVE
        } else {
            TelemetryWrapper.Extra_Value.SAVE
        }
        TelemetryWrapper.changeTravelSettings(TelemetryWrapper.Extra_Value.TRAVEL, category, city.id, city.getTelemetryItemName(), DETAIL_PAGE_SUB_CATEGORY_ID, System.currentTimeMillis().toString(), action)
    }

    fun onIgClicked(igItem: IgUiModel) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.TRAVEL,
            igItem.source,
            igItem.source,
            category,
            igItem.linkUrl.sha256(),
            EXPLORE_CITY_SUB_CATEGORY_ID,
            versionId
        )
        openLink.value = OpenLinkAction(igItem.linkUrl, telemetryData)
    }

    fun onVideoClicked(videoItem: VideoUiModel) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.TRAVEL,
            videoItem.source,
            videoItem.source,
            category,
            videoItem.linkUrl.sha256(),
            EXPLORE_CITY_SUB_CATEGORY_ID,
            versionId
        )
        openLink.value = OpenLinkAction(videoItem.linkUrl, telemetryData)
    }

    fun onWikiClicked(wikiItem: WikiUiModel) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.TRAVEL,
            wikiItem.source,
            wikiItem.source,
            category,
            wikiItem.linkUrl.sha256(),
            EXPLORE_CITY_SUB_CATEGORY_ID,
            versionId
        )
        openLink.value = OpenLinkAction(wikiItem.linkUrl, telemetryData)
    }

    fun onHotelClicked(hotelItem: HotelUiModel) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.TRAVEL,
            hotelItem.source,
            hotelItem.source,
            category,
            hotelItem.linkUrl.sha256(),
            HOTEL_LISTING_SUB_CATEGORY_ID,
            versionId
        )
        openLink.value = OpenLinkAction(hotelItem.linkUrl, telemetryData)
    }

    fun onMoreClicked(headerItem: SectionHeaderUiModel) {
        if (URLUtil.isValidUrl(headerItem.linkUrl)) {
            val telemetryData = ContentTabTelemetryData(
                TelemetryWrapper.Extra_Value.TRAVEL,
                headerItem.source,
                headerItem.source,
                category,
                headerItem.linkUrl.sha256(),
                HOTEL_LISTING_SUB_CATEGORY_ID,
                versionId
            )
            openLink.value = OpenLinkAction(headerItem.linkUrl, telemetryData)
            TelemetryWrapper.openDetailPageMore(TelemetryWrapper.Extra_Value.TRAVEL, category, city.id, city.getTelemetryItemName(), HOTEL_LISTING_SUB_CATEGORY_ID)
        }
    }

    fun onDetailItemScrolled(firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int, isScrollDown: Boolean) {
        if (isScrollDown && firstVisibleItem + visibleItemCount + LOAD_MORE_HOTELS_THRESHOLD >= totalItemCount) {
            loadMoreHotels()
        }
    }

    private fun launchDataLoad(block1: suspend () -> Unit, block2: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                setDataLoadingState(State.Loading)
                block1()
                setDataLoadingState(State.Idle)
                block2()
            } catch (ce: CancellationException) {
                // do nothing
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    private fun backgroundTask(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (ce: CancellationException) {
                // do nothing
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

    data class OpenLinkAction(val url: String, val telemetryData: ContentTabTelemetryData)

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
        private const val EXPLORE_CITY_SUB_CATEGORY_ID = "28"
        private const val HOTEL_LISTING_SUB_CATEGORY_ID = "29"
        const val DETAIL_PAGE_SUB_CATEGORY_ID = "30"
    }
}
