package org.mozilla.rocket.content.travel.data

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_COUNTRY_CODE
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_ID
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_IMAGE_URL
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_NAME
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_NAME_IN_ENGLISH
import org.mozilla.rocket.content.travel.data.BucketListCity.Companion.KEY_TYPE
import org.mozilla.strictmodeviolator.StrictModeViolation

class TravelLocalDataSource(private val appContext: Context) : TravelDataSource {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        })
    }

    override suspend fun getExploreList(): Result<ApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getBucketList(): Result<List<BucketListCity>> = withContext(Dispatchers.IO) {
        return@withContext Success(getBucketListFromPreferences())
    }

    override suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getCityIg(name: String): Result<Ig> {
        return withContext(Dispatchers.Default) {
            val normalizedName = String.format("%stravel", name.replace("\\s".toRegex(), "").toLowerCase())
            Success(Ig(
                    normalizedName,
                    String.format("https://www.instagram.com/explore/tags/%s/", normalizedName)
            ))
        }
    }

    override suspend fun getCityWikiName(name: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getCityWikiImage(name: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getCityWikiExtract(name: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getCityVideos(keyword: String): Result<VideoApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getCityHotels(id: String, type: String, offset: Int): Result<BcHotelApiEntity> {
        TODO("not implemented")
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

    override suspend fun getEnglishName(id: String, type: String): Result<String> {
        TODO("not implemented")
    }

    override suspend fun getMoreHotelsUrl(name: String, id: String, type: String): Result<String> {
        TODO("not implemented")
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

    companion object {
        private const val PREF_NAME = "travel"
        private const val KEY_JSON_STRING_BUCKET_LIST = "bucket_list"
    }
}

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
        it.put(KEY_TYPE, this.type)
        it.put(KEY_NAME_IN_ENGLISH, this.nameInEnglish)
        it.put(KEY_COUNTRY_CODE, this.countryCode)
    }
}