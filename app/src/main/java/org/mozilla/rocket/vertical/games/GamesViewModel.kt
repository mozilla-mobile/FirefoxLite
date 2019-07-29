package org.mozilla.rocket.vertical.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.R
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.ToastMessage

class GamesViewModel : ViewModel() {

    val browserGamesItems = MutableLiveData<List<Item>>()

    val showToast = SingleLiveEvent<ToastMessage>()

    init {
        // TODO: init data from repository
        initFakeData()
    }

    private fun initFakeData() {
        browserGamesItems.value = listOf(
            Item.GameCategory("title 1", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 2", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 3", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 4", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 5", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 6", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 7", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            )),
            Item.GameCategory("title 8", listOf(
                GameItem("Game 1", "https://placeimg.com/200/200/animals"),
                GameItem("Game 2", "https://placeimg.com/200/200/animals"),
                GameItem("Game 3", "https://placeimg.com/200/200/animals"),
                GameItem("Game 4", "https://placeimg.com/200/200/animals"),
                GameItem("Game 5", "https://placeimg.com/200/200/animals"),
                GameItem("Game 6", "https://placeimg.com/200/200/animals")
            ))
        )
    }

    fun onGameItemClicked(gameItem: GameItem) {
        // TODO: testing code, needs to be removed
        showToast.value = ToastMessage(R.string.screenshot_image_viewer_dialog_info_title1, ToastMessage.LENGTH_SHORT, "${gameItem.name}")
    }

    sealed class Item {
        data class GameCategory(val title: String, val gameList: List<GameItem>) : Item()
    }

    data class GameItem(val name: String, val imageUrl: String)
}