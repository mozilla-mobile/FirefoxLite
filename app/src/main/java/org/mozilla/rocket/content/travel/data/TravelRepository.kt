package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result

class TravelRepository(
    private val remoteDataSource: TravelRemoteDataSource,
    private val localDataSource: TravelLocalDataSource
) {

    suspend fun getRunwayItems(): Result<List<RunwayItem>> {
        return localDataSource.getRunwayItems()
    }

    suspend fun getCityCategories(): Result<List<CityCategory>> {
        return localDataSource.getCityCategories()
    }

    suspend fun getBucketList(): Result<List<City>> {
        return localDataSource.getBucketList()
    }

    suspend fun searchCity(keyword: String): Result<List<City>> {
        return localDataSource.searchCity(keyword)
    }

    suspend fun getCityPriceItems(id: Int): Result<List<PriceItem>> {
        return localDataSource.getCityPriceItems(id)
    }

    suspend fun getCityArticles(id: Int): Result<List<Article>> {
        return localDataSource.getCityArticles(id)
    }

    suspend fun getCityHotels(id: Int): Result<List<Hotel>> {
        return localDataSource.getCityHotels(id)
    }
}