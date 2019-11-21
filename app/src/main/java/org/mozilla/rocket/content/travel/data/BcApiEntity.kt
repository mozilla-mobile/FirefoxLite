package org.mozilla.rocket.content.travel.data

import org.json.JSONObject
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
        private const val KEY_TYPE_CITY = "city"

        fun fromJson(jsonString: String?): BcAutocompleteApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val jsonArray = jsonObject.optJSONArray(KEY_RESULT)
                val result =
                        (0 until jsonArray.length())
                                .map { index -> jsonArray.getJSONObject(index) }
                                .filter { jObj -> KEY_TYPE_CITY.equals(jObj.optString(KEY_TYPE)) }
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
    val name: String
) {
    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"

        fun fromJson(jsonObject: JSONObject): BcAutocompleteApiItem =
                BcAutocompleteApiItem(
                    jsonObject.optString(KEY_ID),
                    jsonObject.optString(KEY_NAME)
                )
    }
}

data class BcHotelApiEntity(val result: List<BcHotelApiItem>) : BcApiEntity() {
    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromJson(jsonString: String?): BcHotelApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val jsonArray = jsonObject.optJSONArray(KEY_RESULT)
                val result =
                        (0 until jsonArray.length())
                                .map { index -> jsonArray.getJSONObject(index) }
                                .map { jObj -> BcHotelApiItem.fromJson(jObj) }
                                .filterNot { it == null }

                BcHotelApiEntity(result as List<BcHotelApiItem>)
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
    val description: String,
    val hasFreeWifi: Boolean,
    val price: Float,
    val currency: String,
    val canPayAtProperty: Boolean,
    val linkUrl: String
) {
    companion object {
        private const val KEY_HOTEL_ID = "hotel_id"
        private const val KEY_HOTEL_DATA = "hotel_data"
        private const val KEY_DATA_REVIEW_SCORE = "review_score"
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

        fun fromJson(jsonObject: JSONObject): BcHotelApiItem? {

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

                var minPrice = Float.MAX_VALUE
                for (i in 0 until roomData.length()) {
                    val room = roomData.getJSONObject(i)
                    val info = room.optJSONObject(KEY_ROOM_INFO)
                    val roomMinPrice = info.optDouble(KEY_ROOM_INFO_MIN_PRICE).toFloat()

                    if (minPrice > roomMinPrice && roomMinPrice > 0) {
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
                        hotelData.optString(KEY_DATA_HOTEL_DESCRIPTION),
                        hasFreeWifi,
                        minPrice,
                        hotelData.optString(KEY_DATA_CURRENCY),
                        hotelPayAtProperty,
                        hotelData.optString(KEY_DATA_URL)
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}