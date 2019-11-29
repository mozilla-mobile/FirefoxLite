package org.mozilla.rocket.content.travel.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest
import org.mozilla.rocket.util.toJsonObject
import java.util.Locale

class TravelRemoteDataSource : TravelDataSource {

    override suspend fun getExploreList(): Result<ApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getExploreApiEndpoint(), method = Request.Method.GET),
                            onSuccess = {
                                Result.Success(ApiEntity.fromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get remote travel explore data"
        )
    }

    override suspend fun getBucketList(): Result<List<BucketListCity>> {
        TODO("not implemented")
    }

    override suspend fun searchCity(keyword: String): Result<BcAutocompleteApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getSearchCityApiEndpoint(keyword), method = Request.Method.GET, headers = createHeaders()),
                            onSuccess = {
                                Result.Success(BcAutocompleteApiEntity.fromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get search city result"
        )
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

    override suspend fun getCityVideos(keyword: String): Result<VideoApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getVideosApiEndpoint(keyword), method = Request.Method.GET, headers = createVideoHeaders()),
                            onSuccess = {
                                Result.Success(VideoApiEntity.fromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get video result"
        )
    }

    override suspend fun getCityHotels(id: String, type: String, offset: Int): Result<BcHotelApiEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    require(offset % BOOKING_COM_HOTELS_OFFSET_BASE == 0) { "Offset is not multiple of 100, which means end is reached" }
                    require(type == BcAutocompleteApiEntity.TYPE_CITY || type == BcAutocompleteApiEntity.TYPE_REGION) { "Type not supported" }

                    sendHttpRequest(request = Request(url = getHotelsApiEndpoint(id, type, offset), method = Request.Method.GET, headers = createHeaders()),
                            onSuccess = {
                                Result.Success(BcHotelApiEntity.fromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get hotels result"
        )
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

    private fun getExploreApiEndpoint(): String {
        val exploreApiEndpoint = FirebaseHelper.getFirebase().getRcString(STR_TRAVEL_EXPLORE_ENDPOINT)
        return if (exploreApiEndpoint.isNotEmpty()) {
            if (exploreApiEndpoint.contains("locale=%s")) {
                String.format(exploreApiEndpoint, Locale.getDefault().toLanguageTag())
            } else {
                exploreApiEndpoint
            }
        } else {
            DEFAULT_EXPLORE_URL_ENDPOINT
        }
    }

    private fun getBaseApiEndpoint(): String {
        val bookingComEndpoint = FirebaseHelper.getFirebase().getRcString(STR_BOOKING_COM_ENDPOINT)
        return if (bookingComEndpoint.isNotEmpty()) {
            bookingComEndpoint
        } else {
            DEFAULT_BOOKING_COM_ENDPOINT
        }
    }

    private fun getSearchCityApiEndpoint(keyword: String): String {
        val baseApiEndpoint = getBaseApiEndpoint()
        val lang = Locale.getDefault().toLanguageTag()
        val text = Uri.encode(keyword)
        return "$baseApiEndpoint/$BOOKING_COM_PATH_AUTOCOMPLETE?$BOOKING_COM_QUERY_PARAM_TEXT=$text&$BOOKING_COM_QUERY_PARAM_LANGUAGE=$lang"
    }

    private fun getHotelsApiEndpoint(id: String, type: String, offset: Int): String {
        val baseApiEndpoint = getBaseApiEndpoint()
        val lang = Locale.getDefault().toLanguageTag()
        val queryParamId = if (type == BcAutocompleteApiEntity.TYPE_CITY) { BOOKING_COM_QUERY_PARAM_CITY_IDS } else { BOOKING_COM_QUERY_PARAM_REGION_IDS }
        return "$baseApiEndpoint/$BOOKING_COM_PATH_HOTELS?$queryParamId=$id&$BOOKING_COM_QUERY_PARAM_LANGUAGE=$lang&$BOOKING_COM_QUERY_PARAM_EXTRAS=$BOOKING_COM_QUERY_PARAM_EXTRAS_HOTEL&$BOOKING_COM_QUERY_PARAM_ROWS=$BOOKING_COM_QUERY_PARAM_ROWS_HOTEL&$BOOKING_COM_QUERY_PARAM_OFFSET=$offset"
    }

    private fun createHeaders() = MutableHeaders().apply {
        val authorization = FirebaseHelper.getFirebase().getRcString(STR_BOOKING_COM_AUTHORIZATION)
        if (authorization.isNotEmpty()) {
            set("Authorization", authorization)
        }
    }

    private fun getVideosApiEndpoint(keyword: String): String {
        val videoEndPointFormat = FirebaseHelper.getFirebase().getRcString(STR_VIDEO_ENDPOINT)
        val lang = Locale.getDefault().toLanguageTag()

        val endPointFormat = if (videoEndPointFormat.isNotEmpty())
            videoEndPointFormat
        else
            DEFAULT_VIDEO_ENDPOINT_FORMAT

        return String.format(endPointFormat, keyword, lang)
    }

    private fun createVideoHeaders() = MutableHeaders().apply {
        val apiKey = FirebaseHelper.getFirebase().getRcString(STR_VERTICAL_CLIENT_API_KEY)
        if (apiKey.isNotEmpty()) {
            set("X-API-Key", apiKey)
        }
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
        private const val STR_TRAVEL_EXPLORE_ENDPOINT = "str_travel_explore_endpoint"
        private const val DEFAULT_EXPLORE_URL_ENDPOINT = "https://zerda-dcf76.appspot.com/api/v1/content?locale=en-US&category=travelExplore&tag=global_default"
        private const val STR_BOOKING_COM_ENDPOINT = "str_booking_com_endpoint"
        private const val STR_BOOKING_COM_AUTHORIZATION = "str_booking_com_authorization"
        private const val DEFAULT_BOOKING_COM_ENDPOINT = "https://distribution-xml.booking.com/2.5/json"
        private const val STR_VIDEO_ENDPOINT = "str_travel_video_endpoint"
        private const val STR_VERTICAL_CLIENT_API_KEY = "str_vertical_client_api_key"
        private const val VIDEO_QUERY_PARAM = "%s+Travel"
        private const val DEFAULT_VIDEO_ENDPOINT_FORMAT = "https://zerda-dcf76.appspot.com/api/v1/video?query=%s&locale=%s&limit=5"

        private const val BOOKING_COM_PATH_AUTOCOMPLETE = "autocomplete"
        private const val BOOKING_COM_QUERY_PARAM_TEXT = "text"
        private const val BOOKING_COM_PATH_HOTELS = "hotels"
        private const val BOOKING_COM_QUERY_PARAM_CITY_IDS = "city_ids"
        private const val BOOKING_COM_QUERY_PARAM_REGION_IDS = "region_ids"
        private const val BOOKING_COM_QUERY_PARAM_LANGUAGE = "language"
        private const val BOOKING_COM_QUERY_PARAM_EXTRAS = "extras"
        private const val BOOKING_COM_QUERY_PARAM_ROWS = "rows"
        private const val BOOKING_COM_QUERY_PARAM_ROWS_HOTEL = "100"
        private const val BOOKING_COM_QUERY_PARAM_OFFSET = "offset"
        private const val BOOKING_COM_QUERY_PARAM_EXTRAS_HOTEL = "room_info,%20payment_details,%20hotel_info,%20hotel_photos,%20hotel_facilities,%20hotel_description"
        private const val BOOKING_COM_HOTELS_OFFSET_BASE = 100

        private const val WIKI_JSON_KEY_QUERY = "query"
        private const val WIKI_JSON_KEY_PAGES = "pages"
        private const val WIKI_JSON_KEY_EXTRACT = "extract"
        private const val WIKI_JSON_KEY_ORIGINAL = "original"
        private const val WIKI_JSON_KEY_SOURCE = "source"
        private const val WIKI_IMAGE_API = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles="
        private const val WIKI_EXTRACT_API = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exlimit=1&exsectionformat=plain&exchars=320&explaintext=1&titles="
    }
}