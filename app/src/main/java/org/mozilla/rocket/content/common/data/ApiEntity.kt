package org.mozilla.rocket.content.common.data

import org.json.JSONObject
import org.mozilla.rocket.util.toJsonObject

data class ApiEntity(
    val version: Int,
    val subcategories: List<ApiCategory>
) {
    companion object {
        private const val KEY_RESULT = "result"
        private const val KEY_VERSION = "version"
        private const val KEY_SUBCATEGORIES = "subcategories"

        fun fromJson(jsonString: String?): ApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject().optJSONObject(KEY_RESULT)
                val jsonArray = jsonObject.optJSONArray(KEY_SUBCATEGORIES)
                val subcategories =
                    (0 until jsonArray.length())
                        .map { index -> jsonArray.getJSONObject(index) }
                        .map { jObj -> ApiCategory.fromJson(jObj) }

                ApiEntity(
                    jsonObject.optInt(KEY_VERSION),
                    subcategories
                )
            } else {
                ApiEntity(1, emptyList())
            }
        }
    }
}

data class ApiCategory(
    val componentType: String,
    val subcategoryName: String,
    val subcategoryId: Int,
    val items: List<ApiItem>
) {
    companion object {
        private const val KEY_COMPONENT_TYPE = "componentType"
        private const val KEY_SUBCATEGORY_NAME = "subcategoryName"
        private const val KEY_SUBCATEGORY_ID = "subcategoryId"
        private const val KEY_ITEMS = "items"

        fun fromJson(jsonObject: JSONObject): ApiCategory {
            val jsonArray = jsonObject.optJSONArray(KEY_ITEMS)
            val items =
                (0 until jsonArray.length())
                    .map { index -> jsonArray.getJSONObject(index) }
                    .map { jObj -> ApiItem.fromJson(jObj) }

            return ApiCategory(
                jsonObject.optString(KEY_COMPONENT_TYPE),
                jsonObject.optString(KEY_SUBCATEGORY_NAME),
                jsonObject.optInt(KEY_SUBCATEGORY_ID),
                items
            )
        }
    }
}

data class ApiItem(
    val sourceName: String,
    val image: String,
    val destination: String,
    val title: String,
    val componentId: String,
    val price: String = "",
    var discount: String = "",
    var score: Float = 0F,
    var scoreReviews: Int = 0
) {
    companion object {
        private const val KEY_SOURCE_NAME = "source_name"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_TITLE = "title"
        private const val KEY_COMPONENT_ID = "componentId"
        private const val KEY_PRICE = "price"
        private const val KEY_DISCOUNT = "discount"
        private const val KEY_SCORE = "score"
        private const val KEY_SCORE_REVIEWS = "score_reviews"

        fun fromJson(jsonObject: JSONObject): ApiItem =
            ApiItem(
                jsonObject.optString(KEY_SOURCE_NAME),
                jsonObject.optString(KEY_IMAGE),
                jsonObject.optString(KEY_DESTINATION),
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_PRICE),
                jsonObject.optString(KEY_DISCOUNT),
                jsonObject.optDouble(KEY_SCORE, 0.toDouble()).toFloat(),
                jsonObject.optInt(KEY_SCORE_REVIEWS)
            )
    }
}
