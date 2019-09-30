package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result

class TravelRemoteDataSource : TravelDataSource {

    override suspend fun getRunwayItems(): Result<List<RunwayItem>> {
        TODO("not implemented")
    }

    override suspend fun getCityCategories(): Result<List<CityCategory>> {
        TODO("not implemented")
    }

    override suspend fun getBucketList(): Result<List<City>> {
        TODO("not implemented")
    }

    override suspend fun searchCity(keyword: String): Result<List<City>> {
        TODO("not implemented")
    }

    override suspend fun getCityPriceItems(id: Int): Result<List<PriceItem>> {
        TODO("not implemented")
    }

    override suspend fun getCityArticles(id: Int): Result<List<Article>> {
        TODO("not implemented")
    }

    override suspend fun getCityHotels(id: Int): Result<List<Hotel>> {
        TODO("not implemented")
    }
}