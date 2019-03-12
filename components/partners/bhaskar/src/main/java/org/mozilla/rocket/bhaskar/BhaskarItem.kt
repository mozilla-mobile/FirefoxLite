package org.mozilla.rocket.bhaskar

import org.mozilla.lite.partner.NewsItem

data class BhaskarItem(
    override val id: String,
    override val imageUrl: String,
    override val title: String,
    override val newsUrl: String,
    override val time: Long,
    val summary: String,
    val language: String,
    override val category: String,
    override val subcategory: String,
    val keywords: String,
    val description: String,
    val tags: Array<String>,
    val articleFrom: String,
    val province: String,
    val city: String
) : NewsItem {
    override val source: String = "DainikBhaskar.com"
    override val partner: String = "DainikBhaskar.com"
}