package org.mozilla.rocket.content.travel.data

import org.json.JSONArray
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem

interface TravelDataSource {
    suspend fun getExploreList(): Result<ApiEntity>
    suspend fun getBucketList(): Result<List<BucketListCity>>
    suspend fun searchCity(keyword: String): Result<List<ApiItem>>
    suspend fun getCityPriceItems(name: String): Result<List<PriceItem>>
    suspend fun getCityIg(name: String): Result<Ig>
    suspend fun getCityWikiImage(name: String): Result<String>
    suspend fun getCityWikiExtract(name: String): Result<String>
    suspend fun getCityVideos(name: String): Result<List<Video>>
    suspend fun getCityHotels(name: String): Result<List<Hotel>>
    suspend fun isInBucketList(id: String): Boolean
    suspend fun addToBucketList(city: BucketListCity)
    suspend fun removeFromBucketList(id: String)
}

data class BucketListCity(
    val id: String,
    val imageUrl: String,
    val name: String
) {
    companion object {
        internal const val KEY_ID = "id"
        internal const val KEY_IMAGE_URL = "imageUrl"
        internal const val KEY_NAME = "name"

        fun fromJson(jsonString: String): List<BucketListCity> {
            val items = JSONArray(jsonString)
            return (0 until items.length())
                    .map { index -> items.getJSONObject(index) }
                    .map { item ->
                        BucketListCity(
                            item.optString(KEY_ID),
                            item.optString(KEY_IMAGE_URL),
                            item.optString(KEY_NAME)
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

data class Video(
    val id: String,
    val imageUrl: String,
    val length: Int,
    val title: String,
    val author: String,
    val viewCount: Int,
    val date: String,
    val linkUrl: String
)

data class Hotel(
    val id: Int,
    val imageUrl: String,
    val source: String,
    val name: String,
    val distance: Float,
    val rating: Float,
    val hasFreeWifi: Boolean,
    val price: Float,
    val currency: String,
    val hasFreeCancellation: Boolean,
    val canPayAtProperty: Boolean,
    val linkUrl: String
)