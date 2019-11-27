package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

class TravelRepository(
    private val remoteDataSource: TravelRemoteDataSource,
    private val localDataSource: TravelLocalDataSource
) {

    suspend fun getExploreList(): Result<ApiEntity> {
        return remoteDataSource.getExploreList()
    }

    suspend fun getBucketList(): Result<List<BucketListCity>> {
        return localDataSource.getBucketList()
    }

    suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity> {
        return remoteDataSource.searchCity(keyword)
    }

    suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        return localDataSource.getCityPriceItems(name)
    }

    suspend fun getCityIg(name: String): Result<Ig> {
        return localDataSource.getCityIg(name)
    }

    suspend fun getCityWiki(name: String): Result<Wiki> {
        val resultExtract = remoteDataSource.getCityWikiExtract(name)
        val resultImage = remoteDataSource.getCityWikiImage(name)

        if (resultImage !is Result.Success || resultExtract !is Result.Success) {
            return Result.Error(Exception())
        }

        val wiki = Wiki(resultImage.data, resultExtract.data, WIKI_URL + name)

        return Result.Success(wiki)
    }

    suspend fun getCityVideos(name: String): Result<VideoApiEntity> {
        return remoteDataSource.getCityVideos(name)
    }

    suspend fun getCityHotels(id: String, type: String, offset: Int): Result<BcHotelApiEntity> {
        return remoteDataSource.getCityHotels(id, type, offset)
    }

    suspend fun isInBucketList(id: String): Boolean {
        return localDataSource.isInBucketList(id)
    }

    suspend fun addToBucketList(city: BucketListCity) {
        localDataSource.addToBucketList(city)
    }

    suspend fun removeFromBucketList(id: String) {
        localDataSource.removeFromBucketList(id)
    }

    companion object {
        private const val WIKI_URL = "https://en.wikipedia.org/wiki/"
    }
}