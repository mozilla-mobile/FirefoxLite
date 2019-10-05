package org.mozilla.rocket.content.games.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.games.domain.GetDownloadGameListUseCase
import org.mozilla.rocket.content.games.ui.model.Game
import org.mozilla.rocket.download.SingleLiveEvent

class DownloadGameViewModel(private val getDownloadGameList: GetDownloadGameListUseCase) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _downloadGameItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = getDownloadGameList()
                if (result is Result.Success) {
                    value = GameDataMapper.toGameUiModel(result.data)
                }
            }
        }
    }
    val downloadGameItems: LiveData<List<DelegateAdapter.UiModel>> = _downloadGameItems

    var event = SingleLiveEvent<GameAction>()

    lateinit var selectedGame: Game

    fun onGameItemClicked(gameItem: Game) {
        event.value = GameAction.Install(gameItem.linkUrl)
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
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

    sealed class GameAction {
        data class Install(val url: String) : GameAction()
        data class OpenLink(val url: String) : GameAction()
    }
}