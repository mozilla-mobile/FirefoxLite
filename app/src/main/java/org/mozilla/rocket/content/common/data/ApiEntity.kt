package org.mozilla.rocket.content.common.data

import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.util.optJsonArray
import org.mozilla.rocket.util.toJsonObject

data class ApiEntity(
    val version: Long,
    val subcategories: List<ApiCategory>
) {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_VERSION, version)
        val jsonArray = JSONArray()
        for (subcategory in subcategories) {
            jsonArray.put(subcategory.toJsonObject())
        }
        jsonObject.put(KEY_SUBCATEGORIES, jsonArray)
        return jsonObject
    }

    companion object {
        private const val KEY_VERSION = "version"
        private const val KEY_SUBCATEGORIES = "subcategories"

        fun fromJson(jsonString: String?): ApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val subcategories = jsonObject.optJsonArray(KEY_SUBCATEGORIES) {
                    ApiCategory.fromJson(it)
                }

                ApiEntity(
                    jsonObject.optLong(KEY_VERSION),
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

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_COMPONENT_TYPE, componentType)
        jsonObject.put(KEY_SUBCATEGORY_NAME, subcategoryName)
        jsonObject.put(KEY_SUBCATEGORY_ID, subcategoryId)
        val jsonArray = JSONArray()
        for (apiItem in items) {
            jsonArray.put(apiItem.toJsonObject())
        }
        jsonObject.put(KEY_ITEMS, jsonArray)
        return jsonObject
    }

    companion object {
        private const val KEY_COMPONENT_TYPE = "componentType"
        private const val KEY_SUBCATEGORY_NAME = "subcategoryName"
        private const val KEY_SUBCATEGORY_ID = "subcategoryId"
        private const val KEY_ITEMS = "items"

        fun fromJson(jsonObject: JSONObject): ApiCategory {
            val items = jsonObject.optJsonArray(KEY_ITEMS) { ApiItem.fromJson(it) }
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
    val categoryName: String,
    val subCategoryId: String,
    val image: String,
    val destination: String,
    val title: String,
    val componentId: String,
    val price: String = "",
    var discount: String = "",
    var score: Float = 0F,
    var scoreReviews: String = "",
    var description: String = "",
    var endDate: Long = 0L,
    var destinationType: Int = 0
) {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_SOURCE_NAME, sourceName)
        jsonObject.put(KEY_CATEGORY_NAME, categoryName)
        jsonObject.put(KEY_SUB_CATEGORY_ID, subCategoryId)
        jsonObject.put(KEY_IMAGE, image)
        jsonObject.put(KEY_DESTINATION, destination)
        jsonObject.put(KEY_TITLE, title)
        jsonObject.put(KEY_COMPONENT_ID, componentId)
        jsonObject.put(KEY_PRICE, price)
        jsonObject.put(KEY_DISCOUNT, discount)
        jsonObject.put(KEY_SCORE, score)
        jsonObject.put(KEY_SCORE_REVIEWS, scoreReviews)
        jsonObject.put(KEY_DESCRIPTION, description)
        jsonObject.put(KEY_END_DATE, endDate)
        jsonObject.put(KEY_DESTINATION_TYPE, destinationType)
        return jsonObject
    }

    companion object {
        private const val KEY_SOURCE_NAME = "source_name"
        private const val KEY_CATEGORY_NAME = "category_name"
        private const val KEY_SUB_CATEGORY_ID = "subcategory_id"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_TITLE = "title"
        private const val KEY_COMPONENT_ID = "component_id"
        private const val KEY_PRICE = "price"
        private const val KEY_DISCOUNT = "discount"
        private const val KEY_SCORE = "score"
        private const val KEY_SCORE_REVIEWS = "score_reviews"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_END_DATE = "end_date"
        private const val KEY_DESTINATION_TYPE = "destination_type"

        fun fromJson(jsonObject: JSONObject): ApiItem =
            ApiItem(
                jsonObject.optString(KEY_SOURCE_NAME),
                jsonObject.optString(KEY_CATEGORY_NAME),
                jsonObject.optString(KEY_SUB_CATEGORY_ID),
                jsonObject.optString(KEY_IMAGE),
                jsonObject.optString(KEY_DESTINATION),
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_PRICE),
                jsonObject.optString(KEY_DISCOUNT),
                jsonObject.optDouble(KEY_SCORE, 0.toDouble()).toFloat(),
                jsonObject.optString(KEY_SCORE_REVIEWS),
                jsonObject.optString(KEY_DESCRIPTION),
                jsonObject.optLong(KEY_END_DATE),
                jsonObject.optInt(KEY_DESTINATION_TYPE)
            )
    }
}
