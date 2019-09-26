package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.Result

interface NewsDataSource {
    suspend fun getNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>>
}