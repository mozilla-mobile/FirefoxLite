package org.mozilla.rocket.shopping.search.data

import org.json.JSONArray
import org.json.JSONObject

data class ShoppingSite(
    val title: String,
    val searchUrl: String,
    val displayUrl: String,
    var isEnabled: Boolean
) {
    constructor(obj: JSONObject) : this(
        obj.optString("title"),
        obj.optString("searchUrl"),
        obj.optString("displayUrl"),
        obj.optBoolean("isEnabled", true)
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("searchUrl", searchUrl)
        put("displayUrl", displayUrl)
        put("isEnabled", isEnabled)
    }

    fun contentEquals(shoppingSite: ShoppingSite): Boolean {
        return this.title == shoppingSite.title &&
            this.searchUrl == shoppingSite.searchUrl &&
            this.displayUrl == shoppingSite.displayUrl
    }
}

fun String.toPreferenceSiteList(): List<ShoppingSite> =
    JSONArray(this).run {
        (0 until length())
            .map { index -> optJSONObject(index) }
            .map { jsonObject -> ShoppingSite(jsonObject) }
    }