package org.mozilla.rocket.shopping.search.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

class ShoppingSearchRepository(
    private val remoteDataSource: ShoppingSearchDataSource,
    private val localDataSource: ShoppingSearchDataSource
) {

    private val shoppingSitesData: MutableLiveData<List<ShoppingSite>> = MutableLiveData()

    fun isShoppingSearchEnabled() = remoteDataSource.isShoppingSearchEnabled()

    fun getShoppingSitesData(): LiveData<List<ShoppingSite>> {
        val shoppingSearchSites = localDataSource.getShoppingSites()
        if (shoppingSearchSites.isNotEmpty()) {
            shoppingSitesData.postValue(shoppingSearchSites)
        }

        return shoppingSitesData
    }

    fun updateShoppingSites(shoppingSites: List<ShoppingSite>) =
        localDataSource.updateShoppingSites(shoppingSites)
}

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
}

fun String.toPreferenceSiteList(): List<ShoppingSite> =
    JSONArray(this).run {
        (0 until length())
            .map { index -> optJSONObject(index) }
            .map { jsonObject -> ShoppingSite(jsonObject) }
    }
