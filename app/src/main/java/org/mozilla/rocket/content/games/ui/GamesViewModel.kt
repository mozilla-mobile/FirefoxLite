package org.mozilla.rocket.content.games.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.content.games.ui.adapter.CarouselBanner
import org.mozilla.rocket.content.games.vo.Game
import org.mozilla.rocket.download.SingleLiveEvent
import java.io.InputStream
import java.net.URL

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    private val uiScope = CoroutineScope(Dispatchers.Main)
    val browserGamesState = MutableLiveData<State>()
    val browserGamesItems = MediatorLiveData<List<DelegateAdapter.UiModel>>()
    val premiumGamesItems = MediatorLiveData<List<DelegateAdapter.UiModel>>()
    var isInit = false
    var packageManager: PackageManager? = null

    private val _premiumBanner = gamesRepo.loadPremiumBanner()
    private val _premiumGames = gamesRepo.loadPremiumGames()
    private val _browserBanner = gamesRepo.loadBrowserBanner()
    private val _browserGames = gamesRepo.loadBrowserGames()

    var event = SingleLiveEvent<GameAction>()
    var createShortcutEvent = SingleLiveEvent<GameShortcut>()

    lateinit var selectedGame: Game

    fun canShare(): Boolean {
        return true
    }

    fun canCreateShortCut(): Boolean {
        return selectedGame.type == "Browser"
    }

    fun canRemoveFromList(): Boolean {
        return selectedGame.recentplay == true
    }

    fun canUninstall(): Boolean {
        return selectedGame.type == "Premium"
    }

    init {
        loadData()
        browserGamesItems.addSource(_browserBanner) {
            var tmplist = mutableListOf<DelegateAdapter.UiModel>()
            tmplist.addAll(listOf(CarouselBanner(it)))
            tmplist.addAll(_browserGames.value!!)
            browserGamesItems.value = tmplist
        }

        browserGamesItems.addSource(_browserGames) {
            var tmplist = mutableListOf<DelegateAdapter.UiModel>()
            tmplist.addAll(listOf(CarouselBanner(_browserBanner.value!!)))
            tmplist.addAll(it)
            browserGamesItems.value = tmplist
        }

        premiumGamesItems.addSource(_premiumBanner) {
            var tmplist = mutableListOf<DelegateAdapter.UiModel>()
            tmplist.addAll(listOf(CarouselBanner(it)))
            tmplist.addAll(_premiumGames.value!!)
            premiumGamesItems.value = tmplist
        }

        premiumGamesItems.addSource(_premiumGames) {
            var tmplist = mutableListOf<DelegateAdapter.UiModel>()
            tmplist.addAll(listOf(CarouselBanner(_premiumBanner.value!!)))
            tmplist.addAll(it)
            premiumGamesItems.value = tmplist
        }
    }

    private fun loadData() {
        launchDataLoad {
            gamesRepo.getFakeData()
        }
    }

    fun onGameItemClicked(gameItem: Game) {
        when (gameItem.type) {
            "Premium" -> {
                event.value = GameAction.Install(gameItem.linkUrl)
            }
            "Browser" -> {
                event.value = GameAction.Play(gameItem.linkUrl)
            }
        }
        Handler().postDelayed({
            addGameToRecentPlayList(gameItem)
        }, 1000)
    }

    fun addGameToRecentPlayList(gameItem: Game) {
        gamesRepo.insertRecentPlayGame(gameItem)
    }

    fun removeGameFromRecentPlayList(gameItem: Game) {
        gamesRepo.removeRecentPlayGame(gameItem)
    }

    fun onGameItemLongClicked(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
    }

    fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
        event.value = GameAction.OpenLink(bannerItem.linkUrl)
    }

    fun onRefreshGameListButtonClicked() {
        // TODO: testing code, needs to be removed
        browserGamesItems.value = emptyList()
        loadData()
    }

    fun createShortCut() {
        uiScope.launch {
            val iconBitmap = withContext(Dispatchers.Default) {
                var inputStream = URL(selectedGame.imageUrl).getContent() as InputStream
                BitmapFactory.decodeStream(inputStream)
            }

            createShortcutEvent.value = GameShortcut(selectedGame.name, selectedGame.linkUrl, iconBitmap)
        }
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

    sealed class GameAction {
        data class Play(val url: String) : GameAction()
        data class Install(val url: String) : GameAction()
        data class OpenLink(val url: String) : GameAction()
    }

    data class GameShortcut(val gameName: String, val gameUrl: String, val gameBitmap: Bitmap)
}