package org.mozilla.rocket.content.travel.data

import android.net.Uri
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import java.util.Locale

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
        val resultName = remoteDataSource.getCityWikiName(name)
        val normalizedName = Uri.encode(if (resultName is Result.Success) { resultName.data } else { name })

        val resultExtract = remoteDataSource.getCityWikiExtract(normalizedName)
        val resultImage = remoteDataSource.getCityWikiImage(normalizedName)

        if (resultImage !is Result.Success || resultExtract !is Result.Success) {
            return Result.Error(Exception())
        }

        val wiki = Wiki(resultImage.data, resultExtract.data, String.format(WIKI_URL, Locale.getDefault().language, normalizedName))

        return Result.Success(wiki)
    }

    suspend fun getCityVideos(keyword: String): Result<VideoApiEntity> {
        return remoteDataSource.getCityVideos(keyword)
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

    suspend fun getEnglishName(id: String, type: String): Result<String> {
        return remoteDataSource.getEnglishName(id, type)
    }

    suspend fun getMoreHotelsUrl(name: String, id: String, type: String): Result<String> {
        return remoteDataSource.getMoreHotelsUrl(name, id, type)
    }

    companion object {
        private const val WIKI_URL = "https://%s.wikipedia.org/wiki/%s"
    }
}