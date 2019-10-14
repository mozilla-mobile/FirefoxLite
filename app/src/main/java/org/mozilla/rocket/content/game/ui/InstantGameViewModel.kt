package org.mozilla.rocket.content.game.ui

import android.graphics.Bitmap
import android.view.ContextMenu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.game.domain.AddRecentlyPlayedGameUseCase
import org.mozilla.rocket.content.game.domain.GetBitmapFromImageLinkUseCase
import org.mozilla.rocket.content.game.domain.GetInstantGameListUseCase
import org.mozilla.rocket.content.game.domain.GetRecentlyPlayedGameListUseCase
import org.mozilla.rocket.content.game.ui.model.Game
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.download.SingleLiveEvent

class InstantGameViewModel(
    private val getInstantGameList: GetInstantGameListUseCase,
    private val addRecentlyPlayedGame: AddRecentlyPlayedGameUseCase,
    private val getRecentlyPlayedGameList: GetRecentlyPlayedGameListUseCase,
    private val getBitmapFromImageLinkUseCase: GetBitmapFromImageLinkUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _instantGameItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val instantGameItems: LiveData<List<DelegateAdapter.UiModel>> = _instantGameItems

    private lateinit var selectedGame: Game

    var event = SingleLiveEvent<GameAction>()

    init {
        getGameUiModelList()
    }

    fun onGameItemClicked(gameItem: Game) {
        event.value = GameAction.Play(gameItem.linkUrl)
        addToRecentlyPlayedGameList(gameItem)
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
    }

    fun onCreateContextMenu(menu: ContextMenu) {
        menu.setHeaderTitle(selectedGame.name)
        menu.add(0, R.id.share, 0, R.string.gaming_vertical_menu_option_1)?.setOnMenuItemClickListener {
            onContextMenuClicked(ContextMenuAction.Share)
        }
        menu.add(0, R.id.shortcut, 0, R.string.gaming_vertical_menu_option_2)?.setOnMenuItemClickListener {
            onContextMenuClicked(ContextMenuAction.CreateShortcut)
        }
    }

    fun onRetryButtonClicked() {
        getGameUiModelList()
    }

    private fun getGameUiModelList() {
        launchDataLoad {
            val result = getInstantGameList()
            if (result is Result.Success) {
                val instantGameList = GameDataMapper.toGameUiModel(result.data)
                getRecentlyPlayedCategoryUiModel()?.let {
                    mergeRecentlyPlayedToGameUiModelList(instantGameList, it)
                }
                _instantGameItems.postValue(instantGameList)
            } else if (result is Result.Error) {
                throw (result.exception)
            }
        }
    }

    private suspend fun getRecentlyPlayedCategoryUiModel(): DelegateAdapter.UiModel? {
        val result = getRecentlyPlayedGameList()
        return if (result is Result.Success && result.isNotEmpty) {
            GameDataMapper.toGameUiModel(result.data)[0]
        } else {
            null
        }
    }

    private fun mergeRecentlyPlayedToGameUiModelList(gameUiModelList: List<DelegateAdapter.UiModel>, recentlyPlayedGameListCategory: DelegateAdapter.UiModel) {
        val mergePosition = if (gameUiModelList.isNotEmpty() && gameUiModelList[0] is Runway) {
            1
        } else {
            0
        }
        (gameUiModelList as ArrayList).add(mergePosition, recentlyPlayedGameListCategory)
    }

    private fun addToRecentlyPlayedGameList(gameItem: Game) {
        viewModelScope.launch {
            addRecentlyPlayedGame(GameDataMapper.toApiItem(gameItem))
            getGameUiModelList()
        }
    }

    private fun onContextMenuClicked(contextMenuAction: ContextMenuAction): Boolean {
        when (contextMenuAction) {
            is ContextMenuAction.Share -> {
                event.value = GameAction.Share(selectedGame.linkUrl)
            }
            is ContextMenuAction.CreateShortcut -> {
                viewModelScope.launch {
                    val bitmapResult = getBitmapFromImageLinkUseCase(selectedGame.imageUrl)
                    if (bitmapResult is Result.Success) {
                        event.postValue(GameAction.CreateShortcut(selectedGame.name, selectedGame.linkUrl, bitmapResult.data))
                    }
                }
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
        data class Play(val url: String) : GameAction()
        data class Share(val url: String) : GameAction()
        data class CreateShortcut(val name: String, val url: String, val bitmap: Bitmap) : GameAction()
    }

    sealed class ContextMenuAction {
        object Share : ContextMenuAction()
        object CreateShortcut : ContextMenuAction()
    }
}