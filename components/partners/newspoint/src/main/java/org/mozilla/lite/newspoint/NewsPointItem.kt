package org.mozilla.lite.newspoint

import org.mozilla.lite.partner.NewsItem

data class NewsPointItem(
    override val id: String,
    override val imageUrl: String?,
    override val title: String,
    override val newsUrl: String,
    override val time: Long,
    val imageid: String?,
    override val partner: String,
    val dm: String,
    val pid: Long,
    val lid: Long,
    val lang: String,
    override val category: String,
    val wu: String,
    val pnu: String,
    val fu: String,
    override val subcategory: String,
    val m: String,
    val tags: List<String>
) : NewsItem {
    override val source: String = "Newspoint"
}