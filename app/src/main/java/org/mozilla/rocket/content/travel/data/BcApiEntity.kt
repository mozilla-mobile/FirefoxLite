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