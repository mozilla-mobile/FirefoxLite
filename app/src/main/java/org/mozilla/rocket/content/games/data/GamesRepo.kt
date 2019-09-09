package org.mozilla.rocket.content.games.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.ui.adapter.CarouselBanner
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameItem
import java.util.UUID

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
                CarouselBannerAdapter.BannerItem(
                    UUID.randomUUID().toString(),
                    "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
                    "http://www.gameloft.com/central/category/asphalt/asphalt-9-legends/"
                ),
                CarouselBannerAdapter.BannerItem(
                    UUID.randomUUID().toString(),
                    "http://www.gameloft.com/central/upload/MCB_Blog-FrontRodolex_2048x700.jpg",
                    "http://www.gameloft.com/central/modern-combat-blackout/modern-combat-blackout-coming-nintendo-switch/"
                ),
                CarouselBannerAdapter.BannerItem(
                    UUID.randomUUID().toString(),
                    "http://www.gameloft.com/central/upload/Slider-1.jpg",
                    "http://www.gameloft.com/central/dungeon-hunter/dungeon-hunter-champions-brings-you-epic-action/"
                )
            )),
            GameCategory(UUID.randomUUID().toString(),
                "Game of the week",
                generateTestingGameList1()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Strategy",
                generateTestingGameList2()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Adventure",
                generateTestingGameList1()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Action",
                generateTestingGameList2()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Arcade",
                generateTestingGameList1()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Puzzle & Logic",
                generateTestingGameList2()
            ),
            GameCategory(UUID.randomUUID().toString(),
                "Sport & Racing",
                generateTestingGameList1()
            )
        )

    private fun generateTestingGameList1(): List<GameItem> {
        return listOf(
            GameItem(
                UUID.randomUUID().toString(),
                "BoboiBoy Galaxy Run",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3662/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/boBoiBoyRunFree/?ms_sid=4&phoneId=32225&game=16003785&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Kitchen Bazar",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3796/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/kitchenBazarFree/?ms_sid=4&phoneId=32225&game=16086981&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Ludibubbles",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2920/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/ludibubblesFree/?ms_sid=4&phoneId=32225&game=15153437&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Danger Dash",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3232/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/dangerDashFree/?ms_sid=4&phoneId=32225&game=15487866&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Puzzle Pets: Pairs",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2794/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/puzzlePetsPairsFree/?ms_sid=4&phoneId=32225&game=15084939&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Meow Meow Life",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3598/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/meowMeowLifeFree/?ms_sid=4&phoneId=32225&game=15928205&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser",
                false
            )
        )
    }

    private fun generateTestingGameList2(): List<GameItem> {
        return listOf(
            GameItem(
                UUID.randomUUID().toString(),
                "Castle of Magic",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3624/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/castleOfMagicFree/?ms_sid=4&phoneId=32225&game=15966327&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Ninja UP!",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2817/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/ninjaUpFree/?ms_sid=4&phoneId=32225&game=15118898&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Kite",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2564/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/kiteFree/?ms_sid=4&phoneId=32225&game=14355923&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Pirates: Path of the Buccaneer",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3297/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/piratesPathOfTheBuccaneerFree/?ms_sid=4&phoneId=32225&game=15574419&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "NitroStreet: DragMode",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2794/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/nitroStreetDragModeFree/?ms_sid=4&phoneId=32225&game=15104944&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            ),
            GameItem(
                UUID.randomUUID().toString(),
                "Fantasy Sushi Diver",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2665/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/fantasySushiDiverFree/?ms_sid=4&phoneId=32225&game=14583641&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Premium",
                false
            )
        )
    }
}