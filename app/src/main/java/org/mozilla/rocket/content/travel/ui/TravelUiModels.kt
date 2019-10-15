package org.mozilla.rocket.content.travel.ui

import org.mozilla.rocket.adapter.DelegateAdapter

data class BucketListCityUiModel(
    val id: Int,
    val imageUrl: String,
    val name: String,
    val favorite: Boolean
) : DelegateAdapter.UiModel()

data class ExploreIgUiModel(
    val title: String,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class ExploreWikiUiModel(
    val imageUrl: String,
    val source: String,
    val introduction: String,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class VideoUiModel(
    val id: String,
    val imageUrl: String,
    val length: Int,
    val title: String,
    val author: String,
    val viewCount: Int,
    val date: String,
    val read: Boolean,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class HotelUiModel(
    val imageUrl: String,
    val source: String,
    val name: String,
    val distance: Float,
    val rating: Float,
    val hasFreeWifi: Boolean,
    val price: Float,
    val currency: String,
    val hasFreeCancellation: Boolean,
    val canPayAtProperty: Boolean,
    val linkUrl: String
) : DelegateAdapter.UiModel()
