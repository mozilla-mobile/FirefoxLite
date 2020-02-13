package org.mozilla.rocket.content.travel.data

import org.json.JSONObject
import org.mozilla.rocket.util.getJsonArray
import org.mozilla.rocket.util.toJsonObject

// api entity for Booking.com
abstract class BcApiEntity {
    companion object {
        internal const val KEY_RESULT = "result"
    }
}

data class BcAutocompleteApiEntity(val result: List<BcAutocompleteApiItem>) : BcApiEntity() {
    companion object {
        private const val KEY_TYPE = "type"
        internal const val TYPE_CITY = "city"
        internal const val TYPE_REGION = "region"

        fun fromJson(jsonString: String?): BcAutocompleteApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val jsonArray = jsonObject.optJSONArray(KEY_RESULT)
                val result =
                        (0 until jsonArray.length())
                                .map { index -> jsonArray.getJSONObject(index) }
                                .filter { jObj -> TYPE_CITY == jObj.optString(KEY_TYPE) || TYPE_REGION == jObj.optString(KEY_TYPE) }
                                .map { jObj -> BcAutocompleteApiItem.fromJson(jObj) }
                BcAutocompleteApiEntity(result)
            } else {
                BcAutocompleteApiEntity(emptyList())
            }
        }
    }
}

data class BcAutocompleteApiItem(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val type: String,
    val url: String
) {
    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_COUNTRY_NAME = "country_name"
        private const val KEY_COUNTRY_CODE = "country"
        private const val KEY_TYPE = "type"
        private const val KEY_URL = "url"

        fun fromJson(jsonObject: JSONObject): BcAutocompleteApiItem =
                BcAutocompleteApiItem(
                    jsonObject.optString(KEY_ID),
                    jsonObject.optString(KEY_NAME),
                    jsonObject.optString(KEY_COUNTRY_NAME),
                    jsonObject.optString(KEY_COUNTRY_CODE),
                    jsonObject.optString(KEY_TYPE),
                    jsonObject.optString(KEY_URL)
                )
    }
}

data class BcHotelApiEntity(val result: List<BcHotelApiItem?>) : BcApiEntity() {
    companion object {

        fun fromJson(jsonString: String?, affiliateId: String): BcHotelApiEntity {
            return if (jsonString != null) {
                val result = jsonString.toJsonObject().getJsonArray(KEY_RESULT) {
                    BcHotelApiItem.fromJson(it, affiliateId)
                }
                BcHotelApiEntity(result)
            } else {
                BcHotelApiEntity(emptyList())
            }
        }
    }
}

data class BcHotelApiItem(
    val id: Int,
    val imageUrl: String,
    val name: String,
    val rating: Float,
    val creditCardRequired: Boolean,
    val description: String,
    val hasFreeWifi: Boolean,
    val price: Float,
    val currency: String,
    val canPayAtProperty: Boolean,
    val linkUrl: String,
    val sourceName: String,
    val source: String
) {
    companion object {
        private const val KEY_HOTEL_ID = "hotel_id"
        private const val KEY_HOTEL_DATA = "hotel_data"
        private const val KEY_DATA_REVIEW_SCORE = "review_score"
        private const val KEY_DATA_CREDITCARD_REQUIRED = "creditcard_required"
        private const val KEY_DATA_NAME = "name"
        private const val KEY_DATA_HOTEL_DESCRIPTION = "hotel_description"
        private const val KEY_DATA_HOTEL_PHOTOS = "hotel_photos"
        private const val KEY_DATA_HOTEL_PHOTOS_MAIN = "main_photo"
        private const val KEY_DATA_HOTEL_PHOTOS_URL_ORIGINAL = "url_original"
        private const val KEY_DATA_HOTEL_FACILITIES = "hotel_facilities"
        private const val KEY_DATA_HOTEL_FACILITIES_TYPE_ID = "hotel_facility_type_id"
        private const val KEY_DATA_URL = "url"
        private const val KEY_DATA_CURRENCY = "currency"
        private const val KEY_DATA_PAYMENT_OPTIONS = "payment_options"
        private const val KEY_DATA_PAYMENT_OPTIONS_PAY_AT_PROPERTY = "pay_at_property"
        private const val KEY_ROOM = "room_data"
        private const val KEY_ROOM_INFO = "room_info"
        private const val KEY_ROOM_INFO_MIN_PRICE = "min_price"

        private const val FACILITY_ID_FREE_WIFI = "107"

        private const val SOURCE_NAME = "Booking.com"
        const val SOURCE = "booking.com"

        fun fromJson(jsonObject: JSONObject, affiliateId: String): BcHotelApiItem? {
            try {
                val hotelData = jsonObject.optJSONObject(KEY_HOTEL_DATA)
                val hotelPhotos = hotelData.optJSONArray(KEY_DATA_HOTEL_PHOTOS)
                val hotelFacilities = hotelData.optJSONArray(KEY_DATA_HOTEL_FACILITIES)

                val imageUrl = if (hotelPhotos.length() > 0) {
                    var hotelMainPhotoItem: JSONObject? = null
                    for (i in 0 until hotelPhotos.length()) {
                        val photoItem = hotelPhotos.getJSONObject(i)
                        if (photoItem.optBoolean(KEY_DATA_HOTEL_PHOTOS_MAIN, false)) {
                            hotelMainPhotoItem = photoItem
                            break
                        }
                    }
                    val hotelPhotoItem = hotelMainPhotoItem ?: hotelPhotos.getJSONObject(0)
                    hotelPhotoItem.optString(KEY_DATA_HOTEL_PHOTOS_URL_ORIGINAL)
                } else {
                    ""
                }

                var hasFreeWifi = false
                for (i in 0 until hotelFacilities.length()) {
                    val facilityItem = hotelFacilities.getJSONObject(i)
                    val facilityTypeId = facilityItem.optString(KEY_DATA_HOTEL_FACILITIES_TYPE_ID)
                    hasFreeWifi = facilityTypeId == FACILITY_ID_FREE_WIFI
                    if (hasFreeWifi) break
                }

                val roomData = jsonObject.optJSONArray(KEY_ROOM)

                var minPrice = 0f
                for (i in 0 until roomData.length()) {
                    val room = roomData.getJSONObject(i)
                    val info = room.optJSONObject(KEY_ROOM_INFO)
                    val roomMinPrice = info.optDouble(KEY_ROOM_INFO_MIN_PRICE).toFloat()

                    if ((minPrice == 0f || minPrice > roomMinPrice) && roomMinPrice > 0) {
                        minPrice = roomMinPrice
                    }
                }

                val hotelPayment = hotelData.optJSONObject(KEY_DATA_PAYMENT_OPTIONS)
                val hotelPayAtProperty = hotelPayment.optBoolean(KEY_DATA_PAYMENT_OPTIONS_PAY_AT_PROPERTY, false)

                return BcHotelApiItem(
                        jsonObject.optInt(KEY_HOTEL_ID),
                        imageUrl,
                        hotelData.optString(KEY_DATA_NAME),
                        hotelData.optDouble(KEY_DATA_REVIEW_SCORE).toFloat(),
                        hotelData.optBoolean(KEY_DATA_CREDITCARD_REQUIRED),
                        hotelData.optString(KEY_DATA_HOTEL_DESCRIPTION),
                        hasFreeWifi,
                        minPrice,
                        hotelData.optString(KEY_DATA_CURRENCY),
                        hotelPayAtProperty,
                        hotelData.optString(KEY_DATA_URL) + affiliateId,
                        SOURCE_NAME,
                        SOURCE
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}

data class BcTranslationApiEntity(val result: BcTranslationApiItem) : BcApiEntity() {
    companion object {

        fun fromJson(jsonString: String): BcTranslationApiEntity {
            val jsonObject = jsonString.toJsonObject()
            val jsonArray = jsonObject.optJSONArray(KEY_RESULT)
            val result = BcTranslationApiItem.fromJson(jsonArray[0] as JSONObject)

            return BcTranslationApiEntity(result)
        }
    }
}

data class BcTranslationApiItem(val name: String) {
    companion object {
        private const val KEY_TRANSLATIONS = "translations"
        private const val KEY_NAME = "name"

        fun fromJson(jsonObject: JSONObject): BcTranslationApiItem {
            val translations = jsonObject.optJSONArray(KEY_TRANSLATIONS)
            val name = (translations[0] as JSONObject).optString(KEY_NAME)
            return BcTranslationApiItem(name)
        }
    }
}