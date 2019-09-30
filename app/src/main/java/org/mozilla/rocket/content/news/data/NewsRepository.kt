package org.mozilla.rocket.content.news.data

import org.mozilla.rocket.content.Result

class NewsRepository(
    private val remoteDataSource: NewsDataSource
) {

    suspend fun getNewsItems(category: String, language: String, pages: Int, pageSize: Int): Result<List<NewsItem>> =
        remoteDataSource.getNewsItems(category, language, pages, pageSize)
}