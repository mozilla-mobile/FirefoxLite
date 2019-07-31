package org.mozilla.rocket.vertical.games.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mozilla.rocket.vertical.games.browsergames.BrowserGamesAdapter
import org.mozilla.rocket.vertical.games.browsergames.CarouselBannerAdapter
import org.mozilla.rocket.vertical.games.browsergames.GameCategoryAdapter
import kotlin.random.Random

class GamesRepo {

    suspend fun getFakeData(): List<BrowserGamesAdapter.Item> {
        return withContext(Dispatchers.IO) {
            delay(2000)
            generateFakeData()
        }
    }

    private fun generateFakeData(): List<BrowserGamesAdapter.Item> =
            listOf(
                BrowserGamesAdapter.Item.CarouselBanner(listOf(
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner(),
                        generateFakeBanner()
                )),
                BrowserGamesAdapter.Item.GameCategory("title 1", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 2", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 3", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 4", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 5", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 6", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 7", listOf(
                        generateFakeGame(1),
                        generateFakeGame(2),
                        generateFakeGame(3),
                        generateFakeGame(4),
                        generateFakeGame(5),
                        generateFakeGame(6)
                )),
                BrowserGamesAdapter.Item.GameCategory("title 8", listOf(
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
    private fun generateFakeBanner(): CarouselBannerAdapter.BannerItem =
            getPlaceholderImageUrl(400, 200).run { CarouselBannerAdapter.BannerItem(this, this) }

    // TODO: remove test function
    private fun generateFakeGame(number: Int): GameCategoryAdapter.GameItem =
            getPlaceholderImageUrl(100, 100).run { GameCategoryAdapter.GameItem(number.toString(), this) }
}