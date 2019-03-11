package org.mozilla.lite.newspoint

import org.mozilla.lite.partner.NewsItem

data class NewsPointItem(
    override val id: String,
    override val source: String,
    override val imageUrl: String,
    override val title: String,
    override val newsUrl: String,
    override val time: Long,
    val imageid: String,
    val pn: String,
    val dm: String,
    val pid: Long,
    val lid: Long,
    val lang: String,
    val tn: String,
    val wu: String,
    val pnu: String,
    val fu: String,
    val sec: String,
    val m: String,
    val tags: Array<String>
) : NewsItem