package org.mozilla.rocket.content.news.data

data class NewsItem(
    val title: String,
    val link: String,
    val imageUrl: String?,
    val source: String,
    val publishTime: Long,
    val componentId: String,
    val subCategoryId: String = "99",
    val feed: String = ""
)