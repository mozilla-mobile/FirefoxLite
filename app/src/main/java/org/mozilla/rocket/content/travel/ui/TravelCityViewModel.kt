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

class TravelCityViewModel(
    private val getIg: GetCityIgUseCase,
    private val getWiki: GetCityWikiUseCase,
    private val getVideos: GetCityVideosUseCase,
    private val getHotels: GetCityHotelsUseCase
) : ViewModel() {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _items = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val items: LiveData<List<DelegateAdapter.UiModel>> = _items

    fun getLatestItems(name: String) {
        data.clear()
        launchDataLoad {
            // TODO: add price items

            // add explore
            data.add(SectionUiModel(SectionType.Explore(name)))

            val igResult = getIg(name)
            if (igResult is Result.Success) {
                data.add(TravelMapper.toExploreIgUiModel(igResult.data))
            }

            val wikiResult = getWiki(name)
            if (wikiResult is Result.Success) {
                data.add(TravelMapper.toExploreWikiUiModel(wikiResult.data))
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

            // add hotel
            data.add(SectionUiModel(SectionType.TopHotels("https://www.booking.com/searchresults.html?label=gen173nr-1FCAEoggI46AdIMFgEaOcBiAEBmAEwuAEHyAEM2AEB6AEB-AECiAIBqAIDuAK07-DsBcACAQ;sid=f086a0a5fa31aa51435d73202c1f1ebd;tmpl=searchresults;class_interval=1;dest_id=835;dest_type=region;dtdisc=0;from_sf=1;group_adults=2;group_children=0;inac=0;index_postcard=0;label_click=undef;lang=en-us;no_rooms=1;offset=0;postcard=0;room1=A%2CA;sb_price_type=total;shw_aparth=1;slp_r_match=0;soz=1;src=index;src_elem=sb;srpvid=30882d64a1250024;ss=Bali;ss_all=0;ssb=empty;sshis=0;top_ufis=1&")))

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