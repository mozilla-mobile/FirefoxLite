package org.mozilla.rocket.content.news.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.ui.adapter.NewsSourceLogoUiModel
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel
import java.lang.Exception

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
            news.subCategoryId,
            news.trackingUrl,
            news.trackingId,
            news.trackingData,
            news.attributionUrl
        )
    }

    fun toDataModel(news: DelegateAdapter.UiModel): NewsItem = when (news) {
        is NewsSourceLogoUiModel -> NewsItem.NewsTitleItem(news.resourceId)
        is NewsUiModel -> NewsItem.NewsContentItem(
            news.title,
            news.link,
            news.imageUrl,
            news.source,
            news.publishTime,
            news.componentId,
            news.subCategoryId,
            news.feed,
            news.trackingUrl,
            news.trackingId,
            news.trackingData,
            news.attributionUrl
        )
        else -> throw Exception("Do not support non-news UiModel")
    }
}