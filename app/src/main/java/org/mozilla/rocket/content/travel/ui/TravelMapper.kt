package org.mozilla.rocket.content.travel.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.travel.data.BcHotelApiItem
import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.Ig
import org.mozilla.rocket.content.travel.data.Video
import org.mozilla.rocket.content.travel.data.Wiki
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel.Companion.UNAVAILABLE_LANDMARK_DISTANCE
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityCategoryUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.HotelUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel

object TravelMapper {

    private const val BANNER = "banner"
    private const val SOURCE_WIKI = "Wikipedia"
    private const val SOURCE_BC = "Hotels.com"

    fun toExploreList(apiEntity: ApiEntity): List<DelegateAdapter.UiModel> {
        return apiEntity.subcategories.map { subcategory ->
            if (subcategory.componentType == BANNER) {
                Runway(
                        subcategory.componentType,
                        subcategory.subcategoryName,
                        subcategory.subcategoryId,
                        subcategory.items.map { item -> toRunwayItem(item) }
                )
            } else {
                CityCategoryUiModel(
                        subcategory.componentType,
                        subcategory.subcategoryName,
                        subcategory.subcategoryId,
                        subcategory.items.map { item -> toCityUiModel(item) }
                )
            }
        }
    }

    private fun toRunwayItem(item: ApiItem): RunwayItem =
            RunwayItem(
                item.sourceName,
                item.categoryName,
                item.subCategoryId,
                item.image,
                item.destination,
                item.title,
                item.componentId
            )

    private fun toCityUiModel(item: ApiItem): CityUiModel =
            CityUiModel(
                item.componentId,
                item.image,
                item.title
            )

    fun toBucketListCityUiModel(city: BucketListCity): BucketListCityUiModel =
            BucketListCityUiModel(
                city.id,
                city.imageUrl,
                city.name
            )

    fun toCitySearchResultUiModel(id: String, name: CharSequence): CitySearchResultUiModel =
            CitySearchResultUiModel(
                id,
                name
            )

    fun toExploreIgUiModel(ig: Ig): IgUiModel =
            IgUiModel(
                ig.name,
                ig.linkUrl
            )

    fun toExploreWikiUiModel(wiki: Wiki): WikiUiModel =
            WikiUiModel(
                wiki.imageUrl,
                SOURCE_WIKI,
                wiki.introduction,
                wiki.linkUrl
            )

    fun toVideoUiModel(video: Video, read: Boolean): VideoUiModel =
            VideoUiModel(
                video.id,
                video.imageUrl,
                video.length,
                video.title,
                video.author,
                video.viewCount,
                video.date,
                read,
                video.linkUrl
            )

    fun toHotelUiModel(hotel: BcHotelApiItem): HotelUiModel {

        return HotelUiModel(
                hotel.imageUrl,
                SOURCE_BC,
                hotel.name,
                UNAVAILABLE_LANDMARK_DISTANCE,
                hotel.rating,
                10,
                hotel.hasFreeWifi,
                hotel.price,
                hotel.currency,
                false,
                hotel.canPayAtProperty,
                hotel.linkUrl
        )
    }
}