package org.mozilla.rocket.content.games.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.games.ui.adapter.Game
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonObject

class GamesRepo(private val appContext: Context) {

    suspend fun getBasicGameCategoryList(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getMockGameCategoryList(GameType.BASIC) ?: emptyList()
        }
    }

    suspend fun getPremiumGameCategoryList(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getMockGameCategoryList(GameType.PREMIUM) ?: emptyList()
        }
    }

    // TODO: remove mock data
    private fun getMockGameCategoryList(gameType: GameType): List<DelegateAdapter.UiModel>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.game_mock_items)
                ?.jsonStringToGameCategoryList(gameType)
}

private fun String.jsonStringToGameCategoryList(gameType: GameType): List<DelegateAdapter.UiModel>? {
    return try {
        val jsonObject = this.toJsonObject()
        val jsonArray = jsonObject.optJSONArray("subcategory")
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jObj -> createGameCategory(gameType, jObj) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createGameCategory(gameType: GameType, jsonObject: JSONObject): DelegateAdapter.UiModel {
    return if (jsonObject.optString("component_type") == BANNER) {
        Runway(
            jsonObject.optString("component_type"),
            jsonObject.optString("subcategory_name"),
            jsonObject.optInt("subcategory_id"),
            createRunwayItemList(jsonObject.optJSONArray("items"))
        )
    } else {
        GameCategory(
            jsonObject.optString("component_type"),
            jsonObject.optString("subcategory_name"),
            createGameItemList(gameType, jsonObject.optJSONArray("items"))
        )
    }
}

private fun createRunwayItemList(jsonArray: JSONArray): List<RunwayItem> =
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createRunwayItem(jsonObject) }
                .shuffled()

private fun createGameItemList(gameType: GameType, jsonArray: JSONArray): List<Game> =
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createGameItem(gameType, jsonObject) }
                .shuffled()

private fun createRunwayItem(jsonObject: JSONObject): RunwayItem =
        RunwayItem(
            jsonObject.optString("source"),
            jsonObject.optString("image_url"),
            jsonObject.optString("link_url"),
            "",
            jsonObject.optInt("id").toString()
        )

private fun createGameItem(gameType: GameType, jsonObject: JSONObject): Game =
        Game(
            jsonObject.optLong("id"),
            jsonObject.optString("source"),
            jsonObject.optString("image_url"),
            if (gameType == GameType.BASIC) jsonObject.optString("link_url") else TEST_GAME_APK_URL,
            jsonObject.optString("title"),
            gameType
        )

const val TEST_GAME_APK_URL = "https://github.com/mozilla-tw/FirefoxLite/releases/download/v1.8.0/Firefox-Lite-12883.apk"
const val BANNER = "banner"
const val CARD = "scard"