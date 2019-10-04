package org.mozilla.rocket.content.games.ui

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.games.data.GameRepository
import org.mozilla.rocket.content.games.ui.adapter.Game
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.download.SingleLiveEvent

class GamesViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _instantGameItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = gameRepository.getInstantGameList()
                if (result is Result.Success) {
                    value = GameDataMapper.toGameUiModel(result.data)
                }
            }
        }
    }
    val instantGameItems: LiveData<List<DelegateAdapter.UiModel>> = _instantGameItems

    private val _downloadGameItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = gameRepository.getDownloadGameList()
                if (result is Result.Success) {
                    value = GameDataMapper.toGameUiModel(result.data)
                }
            }
        }
    }
    val downloadGameItems: LiveData<List<DelegateAdapter.UiModel>> = _downloadGameItems

    var event = SingleLiveEvent<GameAction>()
    var createShortcutEvent = SingleLiveEvent<GameShortcut>()

    lateinit var selectedGame: Game

    fun onGameItemClicked(gameItem: Game) {
        when (gameItem.type) {
            GameType.INSTANT -> event.value = GameAction.Play(gameItem.linkUrl)
            GameType.DOWNLOAD -> event.value = GameAction.Install(gameItem.linkUrl)
        }
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
    }

    fun getLatestInstantGames() {
        launchDataLoad {
            val result = gameRepository.getInstantGameList()
            if (result is Result.Success) {
                _instantGameItems.postValue(GameDataMapper.toGameUiModel(result.data))
            }
        }
    }

    fun getLatestDownloadGames() {
        launchDataLoad {
            val result = gameRepository.getDownloadGameList()
            if (result is Result.Success) {
                _downloadGameItems.postValue(GameDataMapper.toGameUiModel(result.data))
            }
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

    sealed class GameAction {
        data class Play(val url: String) : GameAction()
        data class Install(val url: String) : GameAction()
        data class OpenLink(val url: String) : GameAction()
    }

    data class GameShortcut(
        val gameName: String,
        val gameUrl: String,
        val gameBitmap: Bitmap
    )
}