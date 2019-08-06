package org.mozilla.rocket.content.games.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.adapter.CarouselBanner
import org.mozilla.rocket.content.games.adapter.GameCategory
import org.mozilla.rocket.content.games.adapter.GameItem
import kotlin.random.Random

class GamesRepo {

    suspend fun getFakeData(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            delay(2000)
            generateFakeData()
        }
    }

    private fun generateFakeData(): List<DelegateAdapter.UiModel> =
            listOf(
                CarouselBanner(listOf(
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner()
                )),
                GameCategory("title 1", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 2", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 3", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 4", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 5", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 6", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 7", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                GameCategory("title 8", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                ))
            )

    // TODO: remove test function
    private fun getPlaceholderImageUrl(w: Int, h: Int): String =
            "https://placeimg.com/$w/$h/animals?whatever=${Random.nextInt(0, 10)}"

    // TODO: remove test function
    private fun getPlaceholderLinkUrl(): String =
            "https://www.google.com.tw/"

    // TODO: remove test function
    private fun generateFakeBanner(): CarouselBannerAdapter.BannerItem =
            getPlaceholderImageUrl(400, 200).run { CarouselBannerAdapter.BannerItem(this, this) }

    // TODO: remove test function
    private fun generateFakeGame(number: Int): GameItem =
            getPlaceholderImageUrl(100, 100).run { GameItem(number.toString(), this, getPlaceholderLinkUrl()) }
}