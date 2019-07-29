package org.mozilla.rocket.vertical.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.R
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.ToastMessage
import kotlin.random.Random

class GamesViewModel : ViewModel() {

    val browserGamesItems = MutableLiveData<List<Item>>()

    val showToast = SingleLiveEvent<ToastMessage>()

    init {
        // TODO: init data from repository
        initFakeData()
    }

    // TODO: remove test function
    private fun initFakeData() {
        browserGamesItems.value = listOf(
            Item.CarouselBanner(listOf(
                generateFakeBanner(),
                generateFakeBanner(),
                generateFakeBanner(),
                generateFakeBanner(),
                generateFakeBanner()
            )),
            Item.GameCategory("title 1", listOf(
                generateFakeGame(1),
                generateFakeGame(2),
                generateFakeGame(3),
                generateFakeGame(4),
                generateFakeGame(5),
                generateFakeGame(6)
            )),
            Item.GameCategory("title 2", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 3", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 4", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 5", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 6", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 7", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            )),
            Item.GameCategory("title 8", listOf(
                    generateFakeGame(1),
                    generateFakeGame(2),
                    generateFakeGame(3),
                    generateFakeGame(4),
                    generateFakeGame(5),
                    generateFakeGame(6)
            ))
        )
    }

    // TODO: remove test function
    private fun getPlaceholderImageUrl(w: Int, h: Int): String =
            "https://placeimg.com/$w/$h/animals?whatever=${Random.nextInt(0, 10)}"

    // TODO: remove test function
    private fun generateFakeBanner(): BannerItem = getPlaceholderImageUrl(400, 200).run { BannerItem(this, this) }

    // TODO: remove test function
    private fun generateFakeGame(number: Int): GameItem = getPlaceholderImageUrl(100, 100).run { GameItem(number.toString(), this) }

    fun onGameItemClicked(gameItem: GameItem) {
        // TODO: testing code, needs to be removed
        showToast.value = ToastMessage(R.string.screenshot_image_viewer_dialog_info_title1, ToastMessage.LENGTH_SHORT, "${gameItem.name}")
    }

    fun onBannerItemClicked(bannerItem: BannerItem) {
        // TODO: testing code, needs to be removed
        showToast.value = ToastMessage(R.string.screenshot_image_viewer_dialog_info_title1, ToastMessage.LENGTH_SHORT, "${bannerItem.link}")
    }

    sealed class Item {
        data class CarouselBanner(val banners: List<BannerItem>) : Item()
        data class GameCategory(val title: String, val gameList: List<GameItem>) : Item()
    }

    data class BannerItem(val imageUrl: String, val link: String)

    data class GameItem(val name: String, val imageUrl: String)
}