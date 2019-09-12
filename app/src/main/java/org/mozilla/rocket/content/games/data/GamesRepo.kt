package org.mozilla.rocket.content.games.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.vo.Game
import org.mozilla.rocket.content.games.vo.GameCategory
import org.json.JSONArray
import java.util.Scanner

class GamesRepo {
    // mutablelist to cache Gamelist
    private var _browserGameCategoryList = mutableListOf<GameCategory>()
    private var _premiumGameCategoryList = mutableListOf<GameCategory>()

    // mutable LiveData
    private var _premiumBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _premiumGameCategories: MutableLiveData<List<GameCategory>> = MutableLiveData()
    private var _browserBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _browserGameCategories: MutableLiveData<List<GameCategory>> = MutableLiveData()

    // LiveData to return to viewmodel
    private var premiumBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _premiumBanner
    private var premiumGameCategories: LiveData<List<GameCategory>> = _premiumGameCategories
    private var browserBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _browserBanner
    private var browserGameCategories: LiveData<List<GameCategory>> = _browserGameCategories

    // Recently play and Installed game list
    private var _recentPremiumGamelist = mutableListOf<Game>()
    private var _recentBrowserGamelist = mutableListOf<Game>()

    // API for viewmodel to get data
    fun loadPremiumGames() = premiumGameCategories
    fun loadPremiumBanner() = premiumBanner
    fun loadBrowserGames() = browserGameCategories
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
                            _browserGameCategoryList.removeAt(0)
                        }
                    }
                }
                _browserGameCategories.value = _browserGameCategoryList
            }
        }
    }

    fun insertRecentPlayGame(_game: Game) {
        var game = Game(_game.id, _game.name, _game.imageUrl, _game.linkUrl, "", _game.type, true)
        if (game.type == "Browser") {
                if (_recentBrowserGamelist.count() == 0) {
                    _recentBrowserGamelist.add(game)
                _browserGameCategoryList.add(0, GameCategory("Recently played", _recentBrowserGamelist))
                } else {
                    if (_recentBrowserGamelist.contains(game)) {
                        _recentBrowserGamelist.remove(game)
                    }
                    _recentBrowserGamelist.add(0, game)
                }
            _browserGameCategories.value = _browserGameCategoryList
        }
    }

    init {
        initGamesRepo()
    }

    fun initGamesRepo() {
        val inputStream = this.javaClass.classLoader!!.getResourceAsStream("res/raw/gamedata.json")
        val jsonString = Scanner(inputStream).useDelimiter("\\A").next()
        val jsonArray = JSONArray(jsonString)

        for (i in 0..(jsonArray.length() - 1)) {
            var _bannerList = mutableListOf<CarouselBannerAdapter.BannerItem>()
            var _gameList = mutableListOf<GameCategory>()
            var gamesdb = jsonArray.getJSONObject(i)
            val gameType = gamesdb.optString("type")
            val banners = gamesdb.optJSONArray("banner")
            val gamelists = gamesdb.optJSONArray("gamelist")

            // banners
            for (j in 0..(banners.length() - 1)) {
                val banner = banners.optJSONObject(j)
                _bannerList.add(CarouselBannerAdapter.BannerItem(banner.optString("id"), banner.optString("imageUrl"), banner.optString("linkUrl")))
    }

            // game categories
            for (j in 0..(gamelists.length() - 1)) {
                val gamelist = gamelists.optJSONObject(j)
                val gameCategory = gamelist.optString("type")
                val games = gamelist.optJSONArray("games")
                var _games = mutableListOf<Game>()
                // games
                for (k in 0..(games.length() - 1)) {
                    val game = games.optJSONObject(k)

                    _games.add(Game(game.optLong("id"),
                            game.optString("name"),
                            game.optString("imageUrl"),
                            game.optString("linkUrl"),
                            "",
                            gameType,
                            false,
                            false))
                }
                _gameList.add(GameCategory(gameCategory, _games))
            }

            when (gameType) {
                "Premium" -> {
                    _premiumBanner.value = _bannerList
                    _premiumGameCategoryList = _gameList
                    _premiumGameCategories.value = _premiumGameCategoryList.toList()
                }
                "Browser" -> {
                    _browserBanner.value = _bannerList
                    _browserGameCategoryList = _gameList
                    _browserGameCategories.value = _browserGameCategoryList.toList()
                }
            }
        }
    }
}
