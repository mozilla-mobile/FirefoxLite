package org.mozilla.rocket.content.games.ui

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.content.games.ui.adapter.Game
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.download.SingleLiveEvent

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _basicGameItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                value = gamesRepo.getBasicGameCategoryList()
            }
        }
    }
    val basicGameItems: LiveData<List<DelegateAdapter.UiModel>> = _basicGameItems

    private val _premiumGameItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                value = gamesRepo.getPremiumGameCategoryList()
            }
        }
    }
    val premiumGameItems: LiveData<List<DelegateAdapter.UiModel>> = _premiumGameItems

    var event = SingleLiveEvent<GameAction>()
    var createShortcutEvent = SingleLiveEvent<GameShortcut>()

    lateinit var selectedGame: Game

    fun onGameItemClicked(gameItem: Game) {
        when (gameItem.type) {
            GameType.BASIC -> event.value = GameAction.Play(gameItem.linkUrl)
            GameType.PREMIUM -> event.value = GameAction.Install(gameItem.linkUrl)
        }
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
    }

    fun getLatestBasicGames() {
        launchDataLoad {
            _basicGameItems.postValue(gamesRepo.getBasicGameCategoryList())
        }
    }

    fun getLatestPremiumGames() {
        launchDataLoad {
            _premiumGameItems.postValue(gamesRepo.getPremiumGameCategoryList())
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