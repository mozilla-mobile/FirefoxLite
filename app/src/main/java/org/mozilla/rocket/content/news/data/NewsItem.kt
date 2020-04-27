package org.mozilla.rocket.content.news.data

sealed class NewsItem {

    data class NewsContentItem(
        val title: String,
        val link: String,
        val imageUrl: String?,
        val source: String,
        val publishTime: Long,
        val componentId: String,
        val subCategoryId: String = DEFAULT_SUB_CATEGORY_ID,
        val feed: String = "",
        val trackingUrl: String = "",
        val trackingId: String = "",
        val trackingData: String = "",
        val attributionUrl: String = ""
    ) : NewsItem()

    class NewsTitleItem(
        val resId: Int
    ) : NewsItem()

    companion object {
        const val DEFAULT_SUB_CATEGORY_ID = "99"
    }
}