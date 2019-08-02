package org.mozilla.rocket.content.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.adapter.GameItem
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.ToastMessage

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    val browserGamesState = MutableLiveData<State>()
    val browserGamesItems = MutableLiveData<List<DelegateAdapter.UiModel>>()

    val showToast = SingleLiveEvent<ToastMessage>()

    init {
        loadData()
    }

    private fun loadData() {
        launchDataLoad {
            browserGamesItems.value = gamesRepo.getFakeData()
        }
    }

    fun onGameItemClicked(gameItem: GameItem) {
        // TODO: testing code, needs to be removed
        showToast.value = ToastMessage(R.string.screenshot_image_viewer_dialog_info_title1, ToastMessage.LENGTH_SHORT, "${gameItem.name}")
    }

    fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
        // TODO: testing code, needs to be removed
        showToast.value = ToastMessage(R.string.screenshot_image_viewer_dialog_info_title1, ToastMessage.LENGTH_SHORT, "${bannerItem.link}")
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                browserGamesState.value = State.Loading
                block()
                browserGamesState.value = State.Idle
            } catch (t: Throwable) {
                browserGamesState.value = State.Error(t)
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}