package org.mozilla.rocket.content.news.data

import org.json.JSONObject
import org.mozilla.rocket.content.news.data.NewsLanguage.Companion.KEY_LANGUAGE_CODE
import org.mozilla.rocket.content.news.data.NewsLanguage.Companion.KEY_LANGUAGE_NAME
import java.util.Locale

data class NewsLanguage(
    val key: String,
    val code: String,
    val name: String,
    var isSelected: Boolean = false
) {
    val apiId: String
        get() = key.toLowerCase(Locale.getDefault())

    companion object {
        internal const val KEY_LANGUAGE_CODE = "languageCode"
        internal const val KEY_LANGUAGE_NAME = "languageName"

        fun fromJson(jsonString: String): List<NewsLanguage> {
            val result = ArrayList<NewsLanguage>()
            val items = JSONObject(jsonString)
            for (key in items.keys()) {
                //  Log.d("NewsSettingsRepository", key + " - " + items.get(key))
                val item = items.getJSONObject(key)
                val code = item.optString(KEY_LANGUAGE_CODE)
                val name = item.optString(KEY_LANGUAGE_NAME)
                result.add(NewsLanguage(key, code, name))
            }

            return result
        }
    }
}

fun List<NewsLanguage>.toJson(): JSONObject {
    val node = JSONObject()
    for (newsLanguage in this) {
        val item = JSONObject()
        item.put(KEY_LANGUAGE_CODE, newsLanguage.code)
        item.put(KEY_LANGUAGE_NAME, newsLanguage.name)
        node.put(newsLanguage.key, item)
    }

    return node
}

fun NewsLanguage.toJson(): JSONObject {
    val item = JSONObject()
    item.put(KEY_LANGUAGE_CODE, code)
    item.put(KEY_LANGUAGE_NAME, name)
    val node = JSONObject()
    node.put(key, item)

    return node
}