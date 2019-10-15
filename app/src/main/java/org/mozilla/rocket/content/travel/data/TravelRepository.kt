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

    suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        return localDataSource.getCityPriceItems(name)
    }

    suspend fun getCityIg(name: String): Result<Ig> {
        return localDataSource.getCityIg(name)
    }

    suspend fun getCityWiki(name: String): Result<Wiki> {
        return localDataSource.getCityWiki(name)
    }

    suspend fun getCityVideos(name: String): Result<List<Video>> {
        return localDataSource.getCityVideos(name)
    }

    suspend fun getCityHotels(name: String): Result<List<Hotel>> {
        return localDataSource.getCityHotels(name)
    }
}