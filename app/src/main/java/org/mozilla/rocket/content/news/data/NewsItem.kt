package org.mozilla.rocket.content.news.data

data class NewsItem(
    val title: String,
    val link: String,
    val imageUrl: String?,
    val source: String,
    val publishTime: Long,
    val componentId: String,
    val subCategoryId: String = DEFAULT_SUB_CATEGORY_ID,
    val feed: String = ""
) {
    companion object {
        const val DEFAULT_SUB_CATEGORY_ID = "99"
    }
}