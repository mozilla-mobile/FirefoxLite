package org.mozilla.rocket.content.travel.data

import org.json.JSONObject
import org.mozilla.rocket.util.toJsonArray

data class YoutubeApiEntity(val videos: List<YoutubeApiItem>) {
    companion object {
        fun fromJson(jsonString: String?): YoutubeApiEntity {
            return if (jsonString != null) {
                val jsonArray = jsonString.toJsonArray()
                val videos =
                    (0 until jsonArray.length())
                        .map { index -> jsonArray.getJSONObject(index) }
                        .map { jObj -> YoutubeApiItem.fromJson(jObj) }

                YoutubeApiEntity(videos)
            } else {
                YoutubeApiEntity(emptyList())
            }
        }
    }
}

data class YoutubeApiItem(
    val title: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnail: String,
    val duration: Int,
    val link: String,
    val viewCount: Int,
    val componentId: String,
    val source: String
) {
    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_CHANNEL_TITLE = "channelTitle"
        private const val KEY_PUBLISHED_AT = "publishedAt"
        private const val KEY_THUMBNAIL = "thumbnail"
        private const val KEY_DURATION = "duration"
        private const val KEY_LINK = "link"
        private const val KEY_VIEW_COUNT = "viewCount"
        private const val KEY_COMPONENT_ID = "componentId"
        private const val KEY_SOURCE = "source"

        fun fromJson(jsonObject: JSONObject): YoutubeApiItem =
            YoutubeApiItem(
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_CHANNEL_TITLE),
                jsonObject.optString(KEY_PUBLISHED_AT),
                jsonObject.optString(KEY_THUMBNAIL),
                jsonObject.optString(KEY_DURATION).toIntDuration(),
                jsonObject.optString(KEY_LINK),
                jsonObject.optString(KEY_VIEW_COUNT).toInt(),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_SOURCE)
            )
    }
}

private fun String.toIntDuration(): Int {
    val regex = """^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$""".toRegex()
    val (hours, minutes, seconds) = regex.find(this)!!.destructured
    return 60 * (60 * (hours.toIntOrNull() ?: 0) + (minutes.toIntOrNull() ?: 0)) + (seconds.toIntOrNull() ?: 0)
}