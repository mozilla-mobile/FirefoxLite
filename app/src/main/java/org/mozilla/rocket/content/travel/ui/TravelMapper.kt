package org.mozilla.rocket.content.travel.ui

import android.text.Html
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.travel.data.BcHotelApiItem
import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.Ig
import org.mozilla.rocket.content.travel.data.VideoApiItem
import org.mozilla.rocket.content.travel.data.Wiki
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityCategoryUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CityUiModel
import org.mozilla.rocket.content.travel.ui.adapter.HotelUiModel
import org.mozilla.rocket.content.travel.ui.adapter.IgUiModel
import org.mozilla.rocket.content.travel.ui.adapter.VideoUiModel
import org.mozilla.rocket.content.travel.ui.adapter.WikiUiModel
import java.util.regex.Pattern

object TravelMapper {

    private const val BANNER = "banner"

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
                    getExploreCategoryStringResourceId(subcategory.subcategoryId),
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
                item.destinationType,
                item.title,
                item.componentId
            )

    private fun toCityUiModel(item: ApiItem): CityUiModel {
        val pattern = Pattern.compile("(.{2})-(.*)")
        val matcher = pattern.matcher(item.discount) // eg. "vn-ho chi minh city"
        val found = matcher.find()

        val countryCode = if (found) {
            matcher.group(1)
        } else {
            ""
        }

        val nameInEnglish = if (found) {
            matcher.group(2)
        } else {
            ""
        }

        return CityUiModel(
            item.description,
            item.image,
            item.title,
            item.price,
            nameInEnglish,
            countryCode
        )
    }

    fun toBucketListCityUiModel(city: BucketListCity): BucketListCityUiModel =
            BucketListCityUiModel(
                city.id,
                city.imageUrl,
                city.name,
                city.type,
                city.nameInEnglish,
                city.countryCode
            )

    fun toCitySearchResultUiModel(id: String, name: CharSequence, country: String, countryCode: String, type: String): CitySearchResultUiModel =
            CitySearchResultUiModel(
                id,
                name,
                country,
                countryCode,
                type
            )

    fun toExploreIgUiModel(ig: Ig): IgUiModel =
            IgUiModel(
                ig.name,
                ig.linkUrl
            )

    fun toExploreWikiUiModel(wiki: Wiki, sourceName: String): WikiUiModel =
            WikiUiModel(
                wiki.imageUrl,
                sourceName,
                wiki.introduction,
                wiki.linkUrl
            )

    @Suppress("DEPRECATION")
    fun toVideoUiModel(video: VideoApiItem, read: Boolean): VideoUiModel =
            VideoUiModel(
                video.componentId,
                video.thumbnail,
                video.duration,
                Html.fromHtml(video.title).toString(),
                video.channelTitle,
                video.viewCount,
                video.publishedAt,
                read,
                video.link,
                video.source
            )

    fun toHotelUiModel(hotel: BcHotelApiItem): HotelUiModel =
            HotelUiModel(
                hotel.imageUrl,
                hotel.sourceName,
                hotel.name,
                hotel.description,
                hotel.rating,
                hotel.creditCardRequired,
                10,
                hotel.hasFreeWifi,
                hotel.price,
                hotel.currency,
                hotel.canPayAtProperty,
                hotel.linkUrl,
                hotel.source
            )

    private fun getExploreCategoryStringResourceId(subCategoryId: Int): Int =
        when (subCategoryId) {
            26 -> R.string.travel_destination_subcategory_1
            25 -> R.string.travel_destination_subcategory_2
            else -> 0
        }
}