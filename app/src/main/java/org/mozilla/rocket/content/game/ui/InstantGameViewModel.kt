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
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.game.data.GameLocalDataSource.Companion.RECENTLY_PLAYED_SUB_CATEGORY_ID
import org.mozilla.rocket.content.game.domain.AddRecentlyPlayedGameUseCase
import org.mozilla.rocket.content.game.domain.GetBitmapFromImageLinkUseCase
import org.mozilla.rocket.content.game.domain.GetInstantGameListUseCase
import org.mozilla.rocket.content.game.domain.GetRecentlyPlayedGameListUseCase
import org.mozilla.rocket.content.game.domain.RemoveRecentlyPlayedGameUseCase
import org.mozilla.rocket.content.game.domain.SetRecentPlayedSpotlightIsShownUseCase
import org.mozilla.rocket.content.game.domain.ShouldShowRecentPlayedSpotlightUseCase
import org.mozilla.rocket.content.game.ui.model.Game
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.game.ui.model.GameType
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.download.SingleLiveEvent

class InstantGameViewModel(
    private val getInstantGameList: GetInstantGameListUseCase,
    private val addRecentlyPlayedGame: AddRecentlyPlayedGameUseCase,
    private val removeRecentlyPlayedGame: RemoveRecentlyPlayedGameUseCase,
    private val getRecentlyPlayedGameList: GetRecentlyPlayedGameListUseCase,
    private val getBitmapFromImageLinkUseCase: GetBitmapFromImageLinkUseCase,
    private val shouldShowRecentPlayedSpotlightUseCase: ShouldShowRecentPlayedSpotlightUseCase,
    private val setRecentPlayedSpotlightIsShownUseCase: SetRecentPlayedSpotlightIsShownUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _instantGameItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val instantGameItems: LiveData<List<DelegateAdapter.UiModel>> = _instantGameItems

    private lateinit var selectedGame: Game

    var event = SingleLiveEvent<GameAction>()
    val showRecentPlayedSpotlight = SingleLiveEvent<Unit>()
    val dismissRecentPlayedSpotlight = SingleLiveEvent<Unit>()

    var versionId = 0L

    fun requestGameList() {
        getGameUiModelList()
    }

    fun onGameItemClicked(gameItem: Game) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.GAME,
            gameItem.brand,
            gameItem.brand,
            gameItem.category,
            gameItem.componentId,
            gameItem.subCategoryId,
            versionId
        )
        event.value = GameAction.Play(gameItem.linkUrl, telemetryData)
        addToRecentlyPlayedGameList(gameItem)
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        TelemetryWrapper.openContentContextMenuEvent(TelemetryWrapper.Extra_Value.GAME, TelemetryWrapper.Extra_Value.INSTANT_GAME)
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
        if (selectedGame.gameType == GameType.RecentlyPlayed) {
            menu.add(0, R.id.remove, 0, R.string.gaming_vertical_menu_option_3)?.setOnMenuItemClickListener {
                onContextMenuClicked(ContextMenuAction.RemoveFromRecentlyPlayed)
            }
        }
    }

    fun onRetryButtonClicked() {
        getGameUiModelList()
    }

    fun onRecentPlayedSpotlightDismissed() {
        setRecentPlayedSpotlightIsShownUseCase()
    }

    fun onRecentPlayedSpotlightButtonClicked() {
        setRecentPlayedSpotlightIsShownUseCase()
        dismissRecentPlayedSpotlight.call()
        TelemetryWrapper.clickGameShortcutContextualHint()
    }

    private fun getGameUiModelList() {
        launchDataLoad {
            val result = getInstantGameList()
            if (result is Result.Success) {
                versionId = result.data.version
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
        if (recentlyPlayedGameListCategory is GameCategory && recentlyPlayedGameListCategory.items.isNotEmpty()) {
            (gameUiModelList as ArrayList).add(mergePosition, recentlyPlayedGameListCategory)
        }
    }

    fun checkRecentPlayedSpotlight() {
        if (shouldShowRecentPlayedSpotlightUseCase()) {
            showRecentPlayedSpotlight.call()
        }
    }

    private fun addToRecentlyPlayedGameList(gameItem: Game) {
        viewModelScope.launch {
            addRecentlyPlayedGame(GameDataMapper.toApiItem(gameItem.copy(
                subCategoryId = RECENTLY_PLAYED_SUB_CATEGORY_ID.toString()
            )))
            getGameUiModelList()
        }
    }

    private fun onContextMenuClicked(contextMenuAction: ContextMenuAction): Boolean {
        when (contextMenuAction) {
            is ContextMenuAction.Share -> {
                event.value = GameAction.Share(selectedGame.linkUrl)
                TelemetryWrapper.clickContentContextMenuItem(TelemetryWrapper.Extra_Value.SHARE_GAME, TelemetryWrapper.Extra_Value.INSTANT_GAME)
            }
            is ContextMenuAction.CreateShortcut -> {
                viewModelScope.launch {
                    val bitmapResult = getBitmapFromImageLinkUseCase(selectedGame.imageUrl)
                    if (bitmapResult is Result.Success) {
                        event.postValue(GameAction.CreateShortcut(selectedGame.name, selectedGame.linkUrl, bitmapResult.data))
                        TelemetryWrapper.clickContentContextMenuItem(TelemetryWrapper.Extra_Value.CREATE_GAME_SHORTCUT, TelemetryWrapper.Extra_Value.INSTANT_GAME)
                    }
                }
            }
            is ContextMenuAction.RemoveFromRecentlyPlayed -> {
                viewModelScope.launch {
                    removeRecentlyPlayedGame(GameDataMapper.toApiItem(selectedGame))
                    getGameUiModelList()
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
        data class Play(val url: String, val telemetryData: ContentTabTelemetryData) : GameAction()
        data class Share(val url: String) : GameAction()
        data class CreateShortcut(val name: String, val url: String, val bitmap: Bitmap) : GameAction()
    }

    sealed class ContextMenuAction {
        object Share : ContextMenuAction()
        object CreateShortcut : ContextMenuAction()
        object RemoveFromRecentlyPlayed : ContextMenuAction()
    }
}