package org.mozilla.rocket.content.travel.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import org.mozilla.focus.locale.Locales
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

    override suspend fun getCityIg(name: String): Result<Ig> {
        TODO("not implemented")
    }

    override suspend fun getCityWikiName(name: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getWikiNameApiEndpoint(name)),
                            onSuccess = {
                                Result.Success(getWikiNameFromJson(it.body.string()))
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get wiki name"
        )
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
                errorMessage = "Unable to get wiki image"
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
                                Result.Success(BcHotelApiEntity.fromJson(it.body.string(), getBcAffiliateId()))
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

    override suspend fun getEnglishName(id: String, type: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getTranslationApiEndpoint(id, type), method = Request.Method.GET, headers = createHeaders()),
                            onSuccess = {
                                Result.Success(BcTranslationApiEntity.fromJson(it.body.string()).result.name)
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get English name"
        )
    }

    override suspend fun getMoreHotelsUrl(name: String, id: String, type: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
                call = {
                    sendHttpRequest(request = Request(url = getSearchCityApiEndpoint(name), method = Request.Method.GET, headers = createHeaders()),
                            onSuccess = {
                                val apiEntity = BcAutocompleteApiEntity.fromJson(it.body.string())
                                val apiItem = apiEntity.result.first { item -> item.id == id && item.type == type }
                                Result.Success(apiItem.url)
                            },
                            onError = {
                                Result.Error(it)
                            }
                    )
                },
                errorMessage = "Unable to get more hotels url"
        )
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
        val lang = getNormalizedLanguage(Locale.getDefault().toLanguageTag())
        val text = Uri.encode(keyword)
        return "$baseApiEndpoint/$BOOKING_COM_PATH_AUTOCOMPLETE?$BOOKING_COM_QUERY_PARAM_TEXT=$text&$BOOKING_COM_QUERY_PARAM_LANGUAGE=$lang"
    }

    private fun getHotelsApiEndpoint(id: String, type: String, offset: Int): String {
        val baseApiEndpoint = getBaseApiEndpoint()
        val lang = getNormalizedLanguage(Locale.getDefault().toLanguageTag())
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

    private fun getWikiNameApiEndpoint(name: String): String = String.format(WIKI_NAME_API, Locales.getLanguage(Locale.getDefault()), name)

    private fun getWikiNameFromJson(jsonString: String): String {
        val jsonObject = jsonString.toJsonObject()
        val search = jsonObject.optJSONObject(WIKI_JSON_KEY_QUERY).optJSONArray(WIKI_JSON_KEY_SEARCH).optJSONObject(0)
        return search.optString(WIKI_JSON_KEY_TITLE)
    }

    private fun getWikiExtractApiEndpoint(name: String): String = String.format(WIKI_EXTRACT_API, Locales.getLanguage(Locale.getDefault()), Locale.getDefault().toLanguageTag().toLowerCase(), name)

    private fun getWikiExtractFromJson(jsonString: String): String {
        val jsonObject = jsonString.toJsonObject()
        val pages = jsonObject.optJSONObject(WIKI_JSON_KEY_QUERY).optJSONObject(WIKI_JSON_KEY_PAGES)
        val keyIterator = pages.keys()
        val pageContent = pages.optJSONObject(keyIterator.next())
        return pageContent.optString(WIKI_JSON_KEY_EXTRACT)
    }

    private fun getWikiImageApiEndpoint(name: String): String = String.format(WIKI_IMAGE_API, Locales.getLanguage(Locale.getDefault()), name)

    private fun getWikiImageFromJson(jsonString: String): String {
        val jsonObject = jsonString.toJsonObject()
        val pages = jsonObject.optJSONObject(WIKI_JSON_KEY_QUERY).optJSONObject(WIKI_JSON_KEY_PAGES)
        val keyIterator = pages.keys()
        val pageContent = pages.optJSONObject(keyIterator.next())
        return pageContent.optJSONObject(WIKI_JSON_KEY_THUMBNAIL).optString(WIKI_JSON_KEY_SOURCE)
    }

    private fun getTranslationApiEndpoint(id: String, type: String): String {
        val baseApiEndpoint = getBaseApiEndpoint()
        val path = if (type == BcAutocompleteApiEntity.TYPE_CITY) { BOOKING_COM_PATH_CITIES } else { BOOKING_COM_PATH_REGIONS }
        val queryParamId = if (type == BcAutocompleteApiEntity.TYPE_CITY) { BOOKING_COM_QUERY_PARAM_CITY_IDS } else { BOOKING_COM_QUERY_PARAM_REGION_IDS }
        return "$baseApiEndpoint/$path?$queryParamId=$id&languages=en"
    }

    private fun getBcAffiliateId(): String {
        val affiliateId = FirebaseHelper.getFirebase().getRcString(STR_BOOKING_COM_AFFILIATED_ID)
        return if (affiliateId.isNotEmpty())
            affiliateId
        else
            DEFAULT_BOOKING_COM_AFFILIATED_ID
    }

    private fun getNormalizedLanguage(language: String): String {
        val normalizedLang = language.toLowerCase()
        return if (normalizedLang == "zh-hant-tw") { "zh-tw" } else { normalizedLang }
    }

    companion object {
        private const val STR_TRAVEL_EXPLORE_ENDPOINT = "str_travel_explore_endpoint"
        private const val DEFAULT_EXPLORE_URL_ENDPOINT = "https://zerda-dcf76.appspot.com/api/v1/content?locale=en-US&category=travelExplore&tag=global_default"
        private const val STR_BOOKING_COM_ENDPOINT = "str_booking_com_endpoint"
        private const val STR_BOOKING_COM_AUTHORIZATION = "str_booking_com_authorization"
        private const val DEFAULT_BOOKING_COM_ENDPOINT = "https://distribution-xml.booking.com/2.5/json"
        private const val STR_BOOKING_COM_AFFILIATED_ID = "str_booking_com_affiliate_id"
        private const val DEFAULT_BOOKING_COM_AFFILIATED_ID = "?aid=1873270"
        private const val STR_VIDEO_ENDPOINT = "str_travel_video_endpoint"
        private const val STR_VERTICAL_CLIENT_API_KEY = "str_vertical_client_api_key"
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
        private const val BOOKING_COM_PATH_CITIES = "cities"
        private const val BOOKING_COM_PATH_REGIONS = "regions"

        private const val WIKI_JSON_KEY_QUERY = "query"
        private const val WIKI_JSON_KEY_SEARCH = "search"
        private const val WIKI_JSON_KEY_TITLE = "title"
        private const val WIKI_JSON_KEY_PAGES = "pages"
        private const val WIKI_JSON_KEY_EXTRACT = "extract"
        private const val WIKI_JSON_KEY_THUMBNAIL = "thumbnail"
        private const val WIKI_JSON_KEY_SOURCE = "source"
        private const val WIKI_NAME_API = "https://%s.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=1&srprop=timestamp&srsearch=%s"
        private const val WIKI_IMAGE_API = "https://%s.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=1080&titles=%s"
        private const val WIKI_EXTRACT_API = "https://%s.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exlimit=1&exsectionformat=plain&exchars=320&explaintext=1&variant=%s&titles=%s"
    }
}