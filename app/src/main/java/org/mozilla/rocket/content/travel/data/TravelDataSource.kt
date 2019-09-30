package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result

interface TravelDataSource {
    suspend fun getRunwayItems(): Result<List<RunwayItem>>
    suspend fun getCityCategories(): Result<List<CityCategory>>
    suspend fun getBucketList(): Result<List<City>>
    suspend fun searchCity(keyword: String): Result<List<City>>
    suspend fun getCityPriceItems(id: Int): Result<List<PriceItem>>
    suspend fun getCityArticles(id: Int): Result<List<Article>>
    suspend fun getCityHotels(id: Int): Result<List<Hotel>>
}

data class RunwayItem(
    val id: Int,
    val imageUrl: String,
    val linkUrl: String,
    val source: String
)

data class City(
    val id: Int,
    val imageUrl: String,
    val name: String
)

data class CityCategory(
    val id: Int,
    val title: String,
    val cityList: List<City>
)

data class PriceItem(
    val type: String,
    val source: String,
    val price: Float,
    val currency: String,
    val linkUrl: String
)

data class Article(
    val id: Int,
    val imageUrl: String,
    val title: String,
    val source: String,
    val read: Boolean
)

data class Hotel(
    val id: Int,
    val imageUrl: String,
    val source: String,
    val title: String,
    val distance: Float,
    val rating: Float,
    val freeWifi: Boolean,
    val price: Float,
    val currency: String
)