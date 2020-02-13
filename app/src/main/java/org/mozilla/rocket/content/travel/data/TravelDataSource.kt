package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.getJsonArray

interface TravelDataSource {
    suspend fun getExploreList(): Result<ApiEntity>
    suspend fun getBucketList(): Result<List<BucketListCity>>
    suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity>
    suspend fun getCityIg(name: String): Result<Ig>
    suspend fun getCityWikiName(name: String): Result<String>
    suspend fun getCityWikiImage(name: String): Result<String>
    suspend fun getCityWikiExtract(name: String): Result<String>
    suspend fun getCityVideos(keyword: String): Result<VideoApiEntity>
    suspend fun getCityHotels(id: String, type: String, offset: Int): Result<BcHotelApiEntity>
    suspend fun isInBucketList(id: String): Boolean
    suspend fun addToBucketList(city: BucketListCity)
    suspend fun removeFromBucketList(id: String)
    suspend fun getEnglishName(id: String, type: String): Result<String>
    suspend fun getMoreHotelsUrl(name: String, id: String, type: String): Result<String>
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
            return jsonString.getJsonArray {
                BucketListCity(
                    it.optString(KEY_ID),
                    it.optString(KEY_IMAGE_URL),
                    it.optString(KEY_NAME),
                    it.optString(KEY_TYPE),
                    it.optString(KEY_NAME_IN_ENGLISH),
                    it.optString(KEY_COUNTRY_CODE)
                )
            }
        }
    }
}

data class Ig(
    val name: String,
    val linkUrl: String
)

data class Wiki(
    val imageUrl: String,
    val introduction: String,
    val linkUrl: String
)