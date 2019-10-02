package org.mozilla.rocket.content.news.data

import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper

data class NewsProvider(
    val type: String,
    val languagesUrl: String,
    val categoriesUrl: String,
    val newsUrl: String
) {
    fun isNewsPoint() = (type == TYPE_NEWS_POINT)

    companion object {
        private const val STR_NEWS_PROVIDERS = "str_news_providers"
        private const val TYPE_NEWS_POINT = "newspoint"

        fun getNewsProvider(): NewsProvider? {
            val config = FirebaseHelper.getFirebase().getRcString(STR_NEWS_PROVIDERS)
            try {
                val jsonObject = JSONObject(config)
                val type = jsonObject.optString("type")
                val languagesUrl = jsonObject.optString("language_listing_url")
                val categoriesUrl = jsonObject.optString("category_listing_url")
                val newsUrl = jsonObject.optString("news_listing_url")

                return NewsProvider(type, languagesUrl, categoriesUrl, newsUrl)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return null
        }
    }
}