package org.mozilla.rocket.content.travel.data

import org.json.JSONArray
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

interface TravelDataSource {
    suspend fun getExploreList(): Result<ApiEntity>
    suspend fun getBucketList(): Result<List<BucketListCity>>
    suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity>
    suspend fun getCityPriceItems(name: String): Result<List<PriceItem>>
    suspend fun getCityIg(name: String): Result<Ig>
    suspend fun getCityWikiImage(name: String): Result<String>
    suspend fun getCityWikiExtract(name: String): Result<String>
    suspend fun getCityVideos(name: String): Result<VideoApiEntity>
    suspend fun getCityHotels(id: String, type: String, offset: Int): Result<BcHotelApiEntity>
    suspend fun isInBucketList(id: String): Boolean
    suspend fun addToBucketList(city: BucketListCity)
    suspend fun removeFromBucketList(id: String)
    suspend fun getEnglishName(id: String, type: String): Result<String>
}

data class BucketListCity(
    val id: String,
    val imageUrl: String,
    val name: String,
    val type: String,
    val nameInEnglish: String,
    val countryCode: String
) {
    companion object {
        internal const val KEY_ID = "id"
        internal const val KEY_IMAGE_URL = "imageUrl"
        internal const val KEY_NAME = "name"
        internal const val KEY_TYPE = "type"
        internal const val KEY_NAME_IN_ENGLISH = "nameInEnglish"
        internal const val KEY_COUNTRY_CODE = "countryCode"

        fun fromJson(jsonString: String): List<BucketListCity> {
            val items = JSONArray(jsonString)
            return (0 until items.length())
                    .map { index -> items.getJSONObject(index) }
                    .map { item ->
                        BucketListCity(
                            item.optString(KEY_ID),
                            item.optString(KEY_IMAGE_URL),
                            item.optString(KEY_NAME),
                            item.optString(KEY_TYPE),
                            item.optString(KEY_NAME_IN_ENGLISH),
                            item.optString(KEY_COUNTRY_CODE)
                        )
                    }
        }
    }
}

data class PriceItem(
    val type: String,
    val source: String,
    val price: Float,
    val currency: String,
    val linkUrl: String
)

data class Ig(
    val name: String,
    val linkUrl: String
)

data class Wiki(
    val imageUrl: String,
    val introduction: String,
    val linkUrl: String
)