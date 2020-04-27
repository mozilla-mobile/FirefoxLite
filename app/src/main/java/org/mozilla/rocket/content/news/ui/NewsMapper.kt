package org.mozilla.rocket.content.news.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.ui.adapter.NewsSourceLogoUiModel
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel

object NewsMapper {
    fun toUiModel(news: NewsItem): DelegateAdapter.UiModel = when (news) {
        is NewsItem.NewsTitleItem -> NewsSourceLogoUiModel(news.resId)
        is NewsItem.NewsContentItem -> NewsUiModel(
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
}