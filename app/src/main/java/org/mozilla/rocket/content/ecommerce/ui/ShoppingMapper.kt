package org.mozilla.rocket.content.ecommerce.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem

object ShoppingMapper {

    private const val BANNER = "banner"

    fun toDeals(apiEntity: ApiEntity): List<DelegateAdapter.UiModel> {
        return apiEntity.subcategories.map { subcategory ->
            if (subcategory.componentType == BANNER) {
                Runway(
                    subcategory.componentType,
                    subcategory.subcategoryName,
                    subcategory.subcategoryId,
                    subcategory.items.map { dealItem -> toRunwayItem(dealItem) }
                )
            } else {
                ProductCategory(
                    subcategory.componentType,
                    subcategory.subcategoryName,
                    subcategory.subcategoryId,
                    subcategory.items.map { dealItem -> toProductItem(dealItem) }
                )
            }
        }
    }

    private fun toRunwayItem(item: ApiItem): RunwayItem =
        RunwayItem(
            item.sourceName,
            item.image,
            item.destination,
            item.title,
            item.componentId
        )

    private fun toProductItem(item: ApiItem): ProductItem =
        ProductItem(
            item.sourceName,
            item.image,
            item.destination,
            item.title,
            item.componentId,
            item.price,
            item.discount,
            item.score,
            item.scoreReviews
        )

    fun toCoupons(apiEntity: ApiEntity): List<DelegateAdapter.UiModel> {
        return apiEntity.subcategories[0].items
            .map { item ->
                Coupon(
                    item.sourceName,
                    item.image,
                    item.destination,
                    item.title,
                    item.componentId,
                    3L
                )
            }
    }
}