package org.mozilla.rocket.content.travel.data

import org.json.JSONObject
import org.mozilla.rocket.util.getJsonArray

data class VideoApiEntity(val videos: List<VideoApiItem>) {
    companion object {
        fun fromJson(jsonString: String?): VideoApiEntity {
            return if (jsonString != null) {
                val videos = jsonString.getJsonArray { VideoApiItem.fromJson(it) }
                VideoApiEntity(videos)
            } else {
                VideoApiEntity(emptyList())
            }
        }
    }
}

data class VideoApiItem(
    val title: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnail: String,
    val duration: String,
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

        fun fromJson(jsonObject: JSONObject): VideoApiItem =
            VideoApiItem(
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_CHANNEL_TITLE),
                jsonObject.optString(KEY_PUBLISHED_AT),
                jsonObject.optString(KEY_THUMBNAIL),
                jsonObject.optString(KEY_DURATION),
                jsonObject.optString(KEY_LINK),
                jsonObject.optString(KEY_VIEW_COUNT).toInt(),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_SOURCE)
            )
    }
}