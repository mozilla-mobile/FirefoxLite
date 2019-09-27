package org.mozilla.rocket.content.ecommerce.data

import org.json.JSONObject
import org.mozilla.rocket.util.toJsonObject

data class DealEntity(
    val version: Int,
    val subcategories: List<DealCategory>
) {
    companion object {
        private const val KEY_VERSION = "version"
        private const val KEY_SUBCATEGORIES = "subcategories"

        fun fromJson(jsonString: String?): DealEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val jsonArray = jsonObject.optJSONArray(KEY_SUBCATEGORIES)
                val subcategories =
                    (0 until jsonArray.length())
                        .map { index -> jsonArray.getJSONObject(index) }
                        .map { jObj -> DealCategory.fromJson(jObj) }

                DealEntity(
                    jsonObject.optInt(KEY_VERSION),
                    subcategories
                )
            } else {
                DealEntity(1, emptyList())
            }
        }
    }
}

data class DealCategory(
    val componentType: String,
    val subcategoryName: String,
    val subcategoryId: Int,
    val items: List<DealItem>
) {
    companion object {
        private const val KEY_COMPONENT_TYPE = "componentType"
        private const val KEY_SUBCATEGORY_NAME = "subcategoryName"
        private const val KEY_SUBCATEGORY_ID = "subcategoryId"
        private const val KEY_ITEMS = "items"

        fun fromJson(jsonObject: JSONObject): DealCategory {
            val jsonArray = jsonObject.optJSONArray(KEY_ITEMS)
            val items =
                (0 until jsonArray.length())
                    .map { index -> jsonArray.getJSONObject(index) }
                    .map { jObj -> DealItem.fromJson(jObj) }

            return DealCategory(
                jsonObject.optString(KEY_COMPONENT_TYPE),
                jsonObject.optString(KEY_SUBCATEGORY_NAME),
                jsonObject.optInt(KEY_SUBCATEGORY_ID),
                items
            )
        }
    }
}

data class DealItem(
    val source: String,
    val imageUrl: String,
    val linkUrl: String,
    val title: String,
    val componentId: String,
    val price: String = "",
    var discount: String = "",
    var rating: Float = 0F,
    var reviews: Int = 0
) {
    companion object {
        private const val KEY_SOURCE = "source"
        private const val KEY_IMAGE_URL = "imageUrl"
        private const val KEY_LINK_URL = "linkUrl"
        private const val KEY_TITLE = "title"
        private const val KEY_COMPONENT_ID = "componentId"
        private const val KEY_PRICE = "price"
        private const val KEY_DISCOUNT = "discount"
        private const val KEY_RATING = "rating"
        private const val KEY_REVIEWS = "reviews"

        fun fromJson(jsonObject: JSONObject): DealItem =
            DealItem(
                jsonObject.optString(KEY_SOURCE),
                jsonObject.optString(KEY_IMAGE_URL),
                jsonObject.optString(KEY_LINK_URL),
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_PRICE),
                jsonObject.optString(KEY_DISCOUNT),
                jsonObject.optDouble(KEY_RATING).toFloat(),
                jsonObject.optInt(KEY_REVIEWS)
            )
    }
}