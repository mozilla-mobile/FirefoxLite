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
import org.mozilla.rocket.content.common.data.ApiEntity
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

    // TODO: remove mock data
    override suspend fun getExploreList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext Success(
                ApiEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.travel_explore_mock_items))
        )
    }

    override suspend fun getBucketList(): Result<List<BucketListCity>> = withContext(Dispatchers.IO) {
        return@withContext Success(getBucketListFromPreferences())
    }

    override suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity> = withContext(Dispatchers.IO) {
        return@withContext Success(
                BcAutocompleteApiEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.travel_search_mock_items))
        )
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

    override suspend fun getCityWikiImage(name: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getCityWikiExtract(name: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getCityVideos(name: String): Result<List<Video>> {
        return withContext(Dispatchers.IO) {
            Success(getMockVideos() ?: emptyList())
        }
    }

    // TODO: remove mock data
    override suspend fun getCityHotels(name: String): Result<BcHotelApiEntity> = withContext(Dispatchers.IO) {
        return@withContext Success(
                BcHotelApiEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_hotels))
        )
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
    private fun getMockPriceItems(): List<PriceItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_price_items)
                    ?.jsonStringToPriceItems()

    // TODO: remove mock data
    private fun getMockVideos(): List<Video>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.city_videos)
                    ?.jsonStringToVideos()

    companion object {
        private const val PREF_NAME = "travel"
        private const val KEY_JSON_STRING_BUCKET_LIST = "bucket_list"
    }
}

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