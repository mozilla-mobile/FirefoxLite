package org.mozilla.rocket.content.travel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray

class TravelLocalDataSource(private val appContext: Context) : TravelDataSource {

    override suspend fun getRunwayItems(): Result<List<RunwayItem>> {
        return withContext(Dispatchers.IO) {
            Success(getMockRunwayItems() ?: emptyList())
        }
    }

    override suspend fun getCityCategories(): Result<List<CityCategory>> {
        return withContext(Dispatchers.IO) {
            Success(getMockCityCategories() ?: emptyList())
        }
    }

    override suspend fun getBucketList(): Result<List<City>> {
        return withContext(Dispatchers.IO) {
            Success(getMockCityCategories()?.first()?.cityList ?: emptyList())
        }
    }

    override suspend fun searchCity(keyword: String): Result<List<City>> {
        return withContext(Dispatchers.IO) {
            Success(getMockCityCategories()?.first()?.cityList ?: emptyList())
        }
    }

    override suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        return withContext(Dispatchers.IO) {
            Success(getMockPriceItems() ?: emptyList())
        }
    }

    override suspend fun getCityIg(name: String): Result<Ig> {
        return withContext(Dispatchers.Default) {
            val normalizedName = name.replace("\\s".toRegex(), "").toLowerCase()
            Success(Ig(
                    normalizedName,
                    String.format("https://www.instagram.com/explore/tags/%s/", normalizedName)
            ))
        }
    }

    override suspend fun getCityWiki(name: String): Result<Wiki> {
        return withContext(Dispatchers.Default) {
            Success(getMockWiki())
        }
    }

    override suspend fun getCityVideos(name: String): Result<List<Video>> {
        return withContext(Dispatchers.IO) {
            Success(getMockVideos() ?: emptyList())
        }
    }

    override suspend fun getCityHotels(name: String): Result<List<Hotel>> {
        return withContext(Dispatchers.IO) {
            Success(getMockHotels() ?: emptyList())
        }
    }

    // TODO: remove mock data
    private fun getMockRunwayItems(): List<RunwayItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.runway_mock_items)
                    ?.jsonStringToRunwayItems()

    // TODO: remove mock data
    private fun getMockCityCategories(): List<CityCategory>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_categories_items)
                    ?.jsonStringToCityCategories()

    // TODO: remove mock data
    private fun getMockPriceItems(): List<PriceItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_price_items)
                    ?.jsonStringToPriceItems()

    // TODO: remove mock data
    private fun getMockWiki(): Wiki =
            Wiki(
                "https://en.wikipedia.org/wiki/Bali#/media/File:Tanah-Lot_Bali_Indonesia_Pura-Tanah-Lot-01.jpg",
                "Bali is a province of Indonesia and the westernmost of the Lesser Sunda Islands. Located east of Java and west of Lombok, the province includes the island of Bali and a few smaller neighbouring islands, notably Nusa Penida, Nusa Lembongan, and Nusa Ceningan. The provincial capital, Denpasar, is the most populous city in the Lesser Sunda Islands and the second largest, after Makassar, in Eastern Indonesia. Bali is the only Hindu-majority province in Indonesia, with 83.5% of the population adhering to Balinese Hinduism.",
                "https://en.wikipedia.org/wiki/Bali"
            )

    // TODO: remove mock data
    private fun getMockVideos(): List<Video>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_videos)
                    ?.jsonStringToVideos()

    // TODO: remove mock data
    private fun getMockHotels(): List<Hotel>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_hotels)
                    ?.jsonStringToHotels()
}

private fun String.jsonStringToRunwayItems(): List<RunwayItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createRunwayItem(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createRunwayItem(jsonObject: JSONObject): RunwayItem =
        RunwayItem(
            jsonObject.optInt("id"),
            jsonObject.optString("image_url"),
            jsonObject.optString("link_url"),
            jsonObject.optString("source")
        )

private fun String.jsonStringToCityCategories(): List<CityCategory>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createCityCategory(jsonObject) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createCityCategory(jsonObject: JSONObject): CityCategory =
        CityCategory(
            jsonObject.optInt("id"),
            jsonObject.optString("title"),
            createCityItems(jsonObject.optJSONArray("city_list"))
        )

private fun createCityItems(jsonArray: JSONArray): List<City> {
    return (0 until jsonArray.length())
            .map { index -> jsonArray.getJSONObject(index) }
            .map { jsonObject -> createCityItem(jsonObject) }
            .shuffled()
}

private fun createCityItem(jsonObject: JSONObject): City =
        City(
            jsonObject.optInt("id"),
            jsonObject.optString("image_url"),
            jsonObject.optString("name")
        )

private fun String.jsonStringToPriceItems(): List<PriceItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createPriceItem(jsonObject) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createPriceItem(jsonObject: JSONObject): PriceItem =
        PriceItem(
            jsonObject.optString("type"),
            jsonObject.optString("source"),
            jsonObject.optDouble("price", 0.toDouble()).toFloat(),
            jsonObject.optString("currency"),
            jsonObject.optString("link_url")
        )

private fun String.jsonStringToVideos(): List<Video>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createVideo(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createVideo(jsonObject: JSONObject): Video =
        Video(
            jsonObject.optString("id"),
            jsonObject.optString("image_url"),
            jsonObject.optInt("length"),
            jsonObject.optString("title"),
            jsonObject.optString("author"),
            jsonObject.optInt("view_count"),
            jsonObject.optString("date"),
            String.format("https://www.youtube.com/watch?v=%s", jsonObject.optString("id"))
        )

private fun String.jsonStringToHotels(): List<Hotel>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createHotel(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createHotel(jsonObject: JSONObject): Hotel =
        Hotel(
            jsonObject.optInt("id"),
            jsonObject.optString("image_url"),
            jsonObject.optString("source"),
            jsonObject.optString("name"),
            jsonObject.optDouble("distance", 0.toDouble()).toFloat(),
            jsonObject.optDouble("rating", 0.toDouble()).toFloat(),
            jsonObject.optBoolean("has_free_wifi"),
            jsonObject.optDouble("price", 0.toDouble()).toFloat(),
            jsonObject.optString("currency"),
            jsonObject.optBoolean("has_free_cancellation"),
            jsonObject.optBoolean("can_pay_at_property"),
            jsonObject.optString("link_url")
        )