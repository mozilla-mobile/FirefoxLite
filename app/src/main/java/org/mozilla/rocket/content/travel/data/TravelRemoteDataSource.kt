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

    override suspend fun getSectionHeaders(name: String): Result<List<SectionType>> {
        TODO("not implemented")
    }

    override suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        TODO("not implemented")
    }

    override suspend fun getCityIg(name: String): Result<Ig> {
        TODO("not implemented")
    }

    override suspend fun getCityWiki(name: String): Result<Wiki> {
        TODO("not implemented")
    }

    override suspend fun getCityVideos(name: String): Result<List<Video>> {
        TODO("not implemented")
    }

    override suspend fun getCityHotels(name: String): Result<List<Hotel>> {
        TODO("not implemented")
    }
}