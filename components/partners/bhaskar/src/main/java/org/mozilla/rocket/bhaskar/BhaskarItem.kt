package org.mozilla.rocket.bhaskar

import org.mozilla.lite.partner.NewsItem

data class BhaskarItem(
    override val id: String,
    override val source: String,
    override val imageUrl: String,
    override val title: String,
    override val newsUrl: String,
    override val time: Long,
    val summary: String,
    val language: String,
    val category: String,
    val subcategory: String,
    val keywords: String,
    val description: String,
    val tags: Array<String>,
    val articleFrom: String,
    val province: String,
    val city: String
) : NewsItem