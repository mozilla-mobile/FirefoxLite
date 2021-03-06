package org.mozilla.rocket.content.news.data

data class NewsSettings(
    val newsLanguage: NewsLanguage,
    val newsCategories: List<NewsCategory>,
    val shouldEnableRefresh: Boolean,
    val shouldEnableNewsSettings: Boolean
)