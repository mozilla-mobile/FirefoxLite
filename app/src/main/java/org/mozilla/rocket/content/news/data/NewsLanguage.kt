package org.mozilla.rocket.content.news.data

data class NewsLanguage(
    val key: String,
    val code: String,
    val name: String,
    val isSelected: Boolean = false
)

fun NewsLanguage.getApiId(): String {
    return key.toLowerCase()
}