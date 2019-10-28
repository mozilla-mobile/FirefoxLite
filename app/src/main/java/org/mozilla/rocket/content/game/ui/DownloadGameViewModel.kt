package org.mozilla.rocket.content.game.ui

import android.view.ContextMenu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.game.domain.GetDownloadGameListUseCase
import org.mozilla.rocket.content.game.domain.GetMyGameListUseCase
import org.mozilla.rocket.content.game.ui.model.Game
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.game.ui.model.GameType
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.download.SingleLiveEvent

class DownloadGameViewModel(
    private val getDownloadGameList: GetDownloadGameListUseCase,
    private val getMyGameList: GetMyGameListUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _downloadGameItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val downloadGameItems: LiveData<List<DelegateAdapter.UiModel>> = _downloadGameItems

    private lateinit var selectedGame: Game

    var event = SingleLiveEvent<GameAction>()

    var versionId = 0L

    fun requestGameList() {
        getGameUiModelList()
    }

    fun onGameItemClicked(gameItem: Game) {
        event.value = when (gameItem.gameType) {
            is GameType.MyGame -> GameAction.Launch(gameItem.packageName)
            else -> GameAction.Install(gameItem.linkUrl)
        }
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        TelemetryWrapper.openContentContextMenuEvent(TelemetryWrapper.Extra_Value.GAME, TelemetryWrapper.Extra_Value.DOWNLOAD_GAME)
        return false
    }

    fun onCreateContextMenu(menu: ContextMenu) {
        menu.setHeaderTitle(selectedGame.name)
        menu.add(0, R.id.share, 0, R.string.gaming_vertical_menu_option_1)?.setOnMenuItemClickListener {
            onContextMenuClicked(ContextMenuAction.Share)
        }
    }

    fun onRetryButtonClicked() {
        getGameUiModelList()
    }

    private fun getGameUiModelList() {
        launchDataLoad {
            val result = getDownloadGameList()
            if (result is Result.Success) {
                versionId = result.data.version
                val downloadGameList = GameDataMapper.toGameUiModel(result.data)
                getMyGameCategoryUiModel()?.let {
                    mergeMyGameToGameUiModelList(downloadGameList, it)
                }
                _downloadGameItems.postValue(downloadGameList)
            } else if (result is Result.Error) {
                throw (result.exception)
            }
        }
    }

    private suspend fun getMyGameCategoryUiModel(): DelegateAdapter.UiModel? {
        val result = getMyGameList()
        return if (result is Result.Success && result.isNotEmpty) {
            GameDataMapper.toGameUiModel(result.data)[0]
        } else {
            null
        }
    }

    private fun mergeMyGameToGameUiModelList(gameUiModelList: List<DelegateAdapter.UiModel>, myGameListCategory: DelegateAdapter.UiModel) {
        val mergePosition = if (gameUiModelList.isNotEmpty() && gameUiModelList[0] is Runway) {
            1
        } else {
            0
        }
        if (myGameListCategory is GameCategory && myGameListCategory.items.isNotEmpty()) {
            (gameUiModelList as ArrayList).add(mergePosition, myGameListCategory)
        }
    }

    private fun onContextMenuClicked(contextMenuAction: ContextMenuAction): Boolean {
        when (contextMenuAction) {
            is ContextMenuAction.Share -> {
                event.value = GameAction.Share(selectedGame.linkUrl)
                TelemetryWrapper.clickContentContextMenuItem(TelemetryWrapper.Extra_Value.SHARE_GAME, TelemetryWrapper.Extra_Value.DOWNLOAD_GAME)
            }
        }
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
        data class Launch(val packageName: String) : GameAction()
        data class Share(val url: String) : GameAction()
    }

    sealed class ContextMenuAction {
        object Share : ContextMenuAction()
    }
}