package org.mozilla.rocket.content.travel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.toJsonObject

class TravelRemoteDataSource : TravelDataSource {

    override suspend fun getExploreList(): Result<ApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getBucketList(): Result<List<BucketListCity>> {
        TODO("not implemented")
    }

    override suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity> {
        TODO("not implemented")
    }

    override suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        TODO("not implemented")
    }

    override suspend fun getCityIg(name: String): Result<Ig> {
        TODO("not implemented")
    }

    override suspend fun getCityWikiImage(name: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getWikiImageApiEndpoint(name)),
                            onSuccess = {
                                Result.Success(getWikiImageFromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get wiki extract"
        )
    }

    override suspend fun getCityWikiExtract(name: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getWikiExtractApiEndpoint(name)),
                            onSuccess = {
                                Result.Success(getWikiExtractFromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get wiki extract"
        )
    }

    override suspend fun getCityVideos(name: String): Result<List<Video>> {
        TODO("not implemented")
    }

    override suspend fun getCityHotels(name: String): Result<BcHotelApiEntity> {
        TODO("not implemented")
    }

    override suspend fun isInBucketList(id: String): Boolean {
        TODO("not implemented")
    }

    override suspend fun addToBucketList(city: BucketListCity) {
        TODO("not implemented")
    }

    override suspend fun removeFromBucketList(id: String) {
        TODO("not implemented")
    }

    private fun getWikiExtractApiEndpoint(name: String): String = WIKI_EXTRACT_API + name

    private fun getWikiExtractFromJson(jsonString: String): String {
        val jsonObject = jsonString.toJsonObject()
        val pages = jsonObject.optJSONObject(WIKI_JSON_KEY_QUERY).optJSONObject(WIKI_JSON_KEY_PAGES)
        val keyIterator = pages.keys()
        val pageContent = pages.optJSONObject(keyIterator.next())
        return pageContent.optString(WIKI_JSON_KEY_EXTRACT)
    }

    private fun getWikiImageApiEndpoint(name: String): String = WIKI_IMAGE_API + name

    private fun getWikiImageFromJson(jsonString: String): String {
        val jsonObject = jsonString.toJsonObject()
        val pages = jsonObject.optJSONObject(WIKI_JSON_KEY_QUERY).optJSONObject(WIKI_JSON_KEY_PAGES)
        val keyIterator = pages.keys()
        val pageContent = pages.optJSONObject(keyIterator.next())
        return pageContent.optJSONObject(WIKI_JSON_KEY_ORIGINAL).optString(WIKI_JSON_KEY_SOURCE)
    }

    companion object {
        private const val WIKI_JSON_KEY_QUERY = "query"
        private const val WIKI_JSON_KEY_PAGES = "pages"
        private const val WIKI_JSON_KEY_EXTRACT = "extract"
        private const val WIKI_JSON_KEY_ORIGINAL = "original"
        private const val WIKI_JSON_KEY_SOURCE = "source"
        private const val WIKI_IMAGE_API = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles="
        private const val WIKI_EXTRACT_API = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exlimit=1&exsectionformat=plain&exchars=320&explaintext=1&titles="
    }
}