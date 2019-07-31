package org.mozilla.rocket.vertical.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.vertical.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.vertical.games.adapter.GameItem
import org.mozilla.rocket.vertical.games.repository.GamesRepo

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    val browserGamesState = MutableLiveData<State>().apply { value = State.Idle }
    val browserGamesItems = MutableLiveData<List<DelegateAdapter.UIModel>>()

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
            } catch (t: Throwable) {
                browserGamesState.value = State.Error(t)
            } finally {
                browserGamesState.value = State.Idle
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}