package org.mozilla.rocket.content.travel.ui

import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.City
import org.mozilla.rocket.content.travel.data.CityCategory
import org.mozilla.rocket.content.travel.data.Hotel
import org.mozilla.rocket.content.travel.data.Ig
import org.mozilla.rocket.content.travel.data.RunwayItem
import org.mozilla.rocket.content.travel.data.Video
import org.mozilla.rocket.content.travel.data.Wiki
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityCategoryUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel

object TravelMapper {

    private const val BANNER = "banner"
    private const val SOURCE_WIKI = "Wikipedia"

    fun toRunway(items: List<RunwayItem>): Runway =
            Runway(
                BANNER,
                BANNER,
                0,
                items.map {
                    toRunwayItemUiModel(it)
                }
            )

    private fun toRunwayItemUiModel(item: RunwayItem): org.mozilla.rocket.content.common.adapter.RunwayItem =
            org.mozilla.rocket.content.common.adapter.RunwayItem(
                item.source,
                item.category,
                item.subCategoryId,
                item.imageUrl,
                item.linkUrl,
                "",
                item.id.toString()
            )

    fun toCityCategoryUiModel(category: CityCategory): CityCategoryUiModel =
            CityCategoryUiModel(
                category.id,
                category.title,
                category.cityList.map {
                    toCityUiModel(it)
                }
            )

    private fun toCityUiModel(city: City): CityUiModel =
            CityUiModel(
                city.id,
                city.imageUrl,
                city.name
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

    fun toHotelUiModel(hotel: Hotel): HotelUiModel =
            HotelUiModel(
                hotel.imageUrl,
                hotel.source,
                hotel.name,
                hotel.distance,
                hotel.rating,
                hotel.hasFreeWifi,
                hotel.price,
                hotel.currency,
                hotel.hasFreeCancellation,
                hotel.canPayAtProperty,
                hotel.linkUrl
            )
}