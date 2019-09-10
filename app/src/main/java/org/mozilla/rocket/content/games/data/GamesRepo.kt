package org.mozilla.rocket.content.games.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.vo.Game
import org.mozilla.rocket.content.games.vo.GameList
import java.util.UUID

class GamesRepo {

    // mutablelist to cache Banners and Gamelist inmemory
    private var _premiumMutableBanner = mutableListOf<CarouselBannerAdapter.BannerItem>()
    private var _browserMutableBanner = mutableListOf<CarouselBannerAdapter.BannerItem>()
    private var _browserMutableGamelist = mutableListOf<GameList>()
    private var _premiumMutableGamelist = mutableListOf<GameList>()

    // LiveData to return to viewmodel
    private var _premiumBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _premiumGames: MutableLiveData<List<GameList>> = MutableLiveData()
    private var premiumBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _premiumBanner
    private var premiumGames: LiveData<List<GameList>> = _premiumGames
    private var _browserBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _browserGames: MutableLiveData<List<GameList>> = MutableLiveData()
    private var browserBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _browserBanner
    private var browserGames: LiveData<List<GameList>> = _browserGames

    // Recently play and Installed game list
    private var _recentPremiumGamelist = mutableListOf<Game>()
    private var _recentBrowserGamelist = mutableListOf<Game>()

    // API for viewmodel to get data
    fun loadPremiumGames() = premiumGames
    fun loadPremiumBanner() = premiumBanner
    fun loadBrowserGames() = browserGames
    fun loadBrowserBanner() = browserBanner

    suspend fun getFakeData() {
        return withContext(Dispatchers.IO) {
            delay(2000)
        }
    }

    fun removeRecentPlayGame(game: Game) {
        when (game.type) {
            "Browser" -> {
                if (_recentBrowserGamelist.count() > 0) {
                    if (_recentBrowserGamelist.contains(game)) {
                        _recentBrowserGamelist.remove(game)
                        if (_recentBrowserGamelist.count() == 0) {
                            _browserMutableGamelist.removeAt(0)
                        }
                    }
                }
                _browserGames.value = _browserMutableGamelist
            }
            "Premium" -> {
                if (_recentPremiumGamelist.count() > 0) {
                    if (_recentPremiumGamelist.contains(game)) {
                        _recentPremiumGamelist.remove(game)
                        if (_recentPremiumGamelist.count() == 0) {
                            _premiumMutableGamelist.removeAt(0)
                        }
                    }
                }
                _premiumGames.value = _premiumMutableGamelist
            }
        }
    }

    fun insertRecentPlayGame(_game: Game) {
        var game = Game(_game.id, _game.name, _game.imageUrl, _game.linkUrl, _game.type, true)
        when (game.type) {
            "Browser" -> {
                if (_recentBrowserGamelist.count() == 0) {
                    _recentBrowserGamelist.add(game)
                    _browserMutableGamelist.add(0, GameList("Recently played", _recentBrowserGamelist))
                } else {
                    if (_recentBrowserGamelist.contains(game)) {
                        _recentBrowserGamelist.remove(game)
                    }
                    _recentBrowserGamelist.add(0, game)
                }
                _browserGames.value = _browserMutableGamelist
            }
            "Premium" -> {
                if (_recentPremiumGamelist.count() == 0) {
                    _recentPremiumGamelist.add(game)
                    _premiumMutableGamelist.add(0, GameList("My game", _recentPremiumGamelist))
                } else {
                    if (_recentPremiumGamelist.contains(game)) {
                        _recentPremiumGamelist.remove(game)
                    }
                    _recentPremiumGamelist.add(0, game)
                }
                _premiumGames.value = _premiumMutableGamelist
            }
        }
    }

    init {
        initGamesRepo()
    }

    private fun genTestingGameListPremium(): List<Game> {
        return listOf(
            Game(
                UUID.randomUUID(),
                "BoboiBoy Galaxy Run",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3662/default/html5/icon/114/icon.png",
                "https://drive.google.com/uc?authuser=0&id=1Y0spV8GMp_-8RVlmhmaGxEMSEvBXuyZt&export=download",
                "Premium"
            ),
            Game(
                UUID.randomUUID(),
                "Kitchen Bazar",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3796/default/html5/icon/114/icon.png",
                "https://drive.google.com/file/d/1Y0spV8GMp_-8RVlmhmaGxEMSEvBXuyZt/view?usp=sharing",
                "Premium"
            ),
            Game(
                UUID.randomUUID(),
                "Ludibubbles",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2920/default/html5/icon/114/icon.png",
                "http://down1.koplayer.com/autoapk/Vinh%20Hang%20Ky%20Nguyen/Vinh%20Hang%20Ky%20Nguyen_v3.22.3.apk",
                "Premium"
            ),
            Game(
                UUID.randomUUID(),
                "Danger Dash",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3232/default/html5/icon/114/icon.png",
                "http://down.koplayer.com/autoApk/com.ea.game.fifa15_row/com.ea.game.fifa15_row-170.apk",
                "Premium"
            ),
            Game(
                UUID.randomUUID(),
                "Puzzle Pets: Pairs",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2794/default/html5/icon/114/icon.png",
                "http://down.koplayer.com/autoApk/com.robtopx.geometryjumplite/com.robtopx.geometryjumplite-30.apk",
                "Premium"
            ),
            Game(
                UUID.randomUUID(),
                "Meow Meow Life",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3598/default/html5/icon/114/icon.png",
                "http://down1.koplayer.com/autoapk/com.igg.castleclash/com.igg.castleclash-1200950.apk",
                "Premium"
            )
        )
    }

    private fun genTestingGameListBrowser(): List<Game> {
        return listOf(
            Game(
                UUID.randomUUID(),
                "Castle of Magic",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3624/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/castleOfMagicFree/?ms_sid=4&phoneId=32225&game=15966327&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            ),
            Game(
                UUID.randomUUID(),
                "Ninja UP!",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2817/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/ninjaUpFree/?ms_sid=4&phoneId=32225&game=15118898&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            ),
            Game(
                UUID.randomUUID(),
                "Kite",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2564/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/kiteFree/?ms_sid=4&phoneId=32225&game=14355923&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            ),
            Game(
                UUID.randomUUID(),
                "Pirates: Path of the Buccaneer",
                "https://media07-gl-ssl-gzip.gameloft.com/products/3297/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/piratesPathOfTheBuccaneerFree/?ms_sid=4&phoneId=32225&game=15574419&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            ),
            Game(
                UUID.randomUUID(),
                "NitroStreet: DragMode",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2794/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/nitroStreetDragModeFree/?ms_sid=4&phoneId=32225&game=15104944&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            ),
            Game(
                UUID.randomUUID(),
                "Fantasy Sushi Diver",
                "https://media07-gl-ssl-gzip.gameloft.com/products/2665/default/html5/icon/114/icon.png",
                "https://cdn.ludigames.com/h5/fantasySushiDiverFree/?ms_sid=4&phoneId=32225&game=14583641&fromPartner=gameloft&sv=126ehy3kvewfvbn64lr4zs3rw&c=206&utm_source=gameloft&utm_medium=bookmark&utm_campaign=PI39",
                "Browser"
            )
        )
    }

    private fun initGamesRepo() {
        _premiumMutableBanner = mutableListOf(
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
                "http://www.gameloft.com/central/category/asphalt/asphalt-9-legends/"),
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/MCB_Blog-FrontRodolex_2048x700.jpg",
                "http://www.gameloft.com/central/modern-combat-blackout/modern-combat-blackout-coming-nintendo-switch/"),
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/Slider-1.jpg",
                "http://www.gameloft.com/central/dungeon-hunter/dungeon-hunter-champions-brings-you-epic-action/"))
        _premiumBanner.value = _premiumMutableBanner.toList()
        _premiumMutableGamelist = mutableListOf(
            GameList("Game of the week", genTestingGameListPremium()),
            GameList("Strategy", genTestingGameListPremium()),
            GameList("Adventure", genTestingGameListPremium()),
            GameList("Action", genTestingGameListPremium()),
            GameList("Arcade", genTestingGameListPremium()),
            GameList("Puzzle & Logic", genTestingGameListPremium()),
            GameList("Sport & Racing", genTestingGameListPremium()))
        _premiumGames.value = _premiumMutableGamelist.toList()
        _browserMutableBanner = mutableListOf(
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
                "http://www.gameloft.com/central/category/asphalt/asphalt-9-legends/"),
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/MCB_Blog-FrontRodolex_2048x700.jpg",
                "http://www.gameloft.com/central/modern-combat-blackout/modern-combat-blackout-coming-nintendo-switch/"),
            CarouselBannerAdapter.BannerItem(UUID.randomUUID().toString(),
                "http://www.gameloft.com/central/upload/Slider-1.jpg",
                "http://www.gameloft.com/central/dungeon-hunter/dungeon-hunter-champions-brings-you-epic-action/"))

        _browserBanner.value = _browserMutableBanner.toList()
        _browserMutableGamelist = mutableListOf(
            GameList("Game of the week", genTestingGameListBrowser()),
            GameList("Strategy", genTestingGameListBrowser()),
            GameList("Adventure", genTestingGameListBrowser()),
            GameList("Action", genTestingGameListBrowser()),
            GameList("Arcade", genTestingGameListBrowser()),
            GameList("Puzzle & Logic", genTestingGameListBrowser()),
            GameList("Sport & Racing", genTestingGameListBrowser()))
        _browserGames.value = _browserMutableGamelist.toList()
    }
}
