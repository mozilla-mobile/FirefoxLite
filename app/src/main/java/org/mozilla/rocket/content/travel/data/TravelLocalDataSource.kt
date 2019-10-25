package org.mozilla.rocket.content.travel.data

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_ID
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_IMAGE_URL
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_NAME
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray
import org.mozilla.strictmodeviolator.StrictModeViolation

class TravelLocalDataSource(private val appContext: Context) : TravelDataSource {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

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

    override suspend fun getBucketList(): Result<List<BucketListCity>> = withContext(Dispatchers.IO) {
        return@withContext Success(getBucketListFromPreferences())
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

    override suspend fun isInBucketList(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            getBucketListFromPreferences().find { it.id.equals(id) } != null
        }
    }

    override suspend fun addToBucketList(city: BucketListCity) = withContext(Dispatchers.IO) {
        val bucketList = getBucketListFromPreferences()
        if (bucketList.find { it.id.equals(city.id) } == null) {
            bucketList.add(city)
            preference.edit().putString(KEY_JSON_STRING_BUCKET_LIST, bucketList.toJsonString()).apply()
        }
    }

    override suspend fun removeFromBucketList(id: String) = withContext(Dispatchers.IO) {
        preference.edit().putString(KEY_JSON_STRING_BUCKET_LIST, getBucketListFromPreferences().filterNot { it.id.equals(id) }.toJsonString()).apply()
    }

    private fun getBucketListFromPreferences(): MutableList<BucketListCity> {
        return try {
            val jsonString = preference.getString(KEY_JSON_STRING_BUCKET_LIST, "") ?: ""
            if (TextUtils.isEmpty(jsonString)) {
                arrayListOf()
            } else {
                ArrayList<BucketListCity>().apply { addAll(BucketListCity.fromJson(jsonString)) }
            }
        } catch (e: Exception) {
            arrayListOf()
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
                "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Tanah-Lot_Bali_Indonesia_Pura-Tanah-Lot-01.jpg/2560px-Tanah-Lot_Bali_Indonesia_Pura-Tanah-Lot-01.jpg",
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

    companion object {
        private const val PREF_NAME = "travel"
        private const val KEY_JSON_STRING_BUCKET_LIST = "bucket_list"
    }
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
            jsonObject.optString("source"),
            jsonObject.optString("category_name"),
            jsonObject.optString("subcategory_id")
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
            jsonObject.optString("id"),
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

private fun List<BucketListCity>.toJsonString(): String {
    val jsonArray = JSONArray()
    for (city in this) {
        jsonArray.put(city.toJson())
    }
    return jsonArray.toString()
}

private fun BucketListCity.toJson(): JSONObject {
    return JSONObject().let {
        it.put(KEY_ID, this.id)
        it.put(KEY_IMAGE_URL, this.imageUrl)
        it.put(KEY_NAME, this.name)
    }
}