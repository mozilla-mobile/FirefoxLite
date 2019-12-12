package org.mozilla.rocket.content.news.ui

import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel

object NewsMapper {
    fun toNewsUiModel(news: NewsItem): NewsUiModel =
            NewsUiModel(
                news.title,
                news.link,
                news.source,
                news.imageUrl,
                news.publishTime,
                news.componentId,
                news.feed,
                news.subCategoryId
            )
}