package org.mozilla.lite.partner

interface NewsItem {
    val id: String
    val source: String
    val imageUrl: String
    val title: String
    val newsUrl: String
    val time: Long
}