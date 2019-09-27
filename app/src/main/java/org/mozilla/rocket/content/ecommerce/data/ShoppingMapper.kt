package org.mozilla.rocket.content.ecommerce.data

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem

object ShoppingMapper {

    private const val BANNER = "banner"

    fun toDeals(entity: DealEntity): List<DelegateAdapter.UiModel> {
        return entity.subcategories.map { subcategory ->
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

    private fun toRunwayItem(item: DealItem): RunwayItem =
            RunwayItem(
                item.source,
                item.imageUrl,
                item.linkUrl,
                item.title,
                item.componentId
            )

    private fun toProductItem(item: DealItem): ProductItem =
            ProductItem(
                item.source,
                item.imageUrl,
                item.linkUrl,
                item.title,
                item.componentId,
                item.price,
                item.discount,
                item.rating,
                item.reviews
            )

    fun toCoupons(entity: DealEntity): List<DelegateAdapter.UiModel> {
        return entity.subcategories[0].items
                .map { item ->
                    Coupon(
                        item.source,
                        item.imageUrl,
                        item.linkUrl,
                        item.title,
                        item.componentId,
                        3L
                    )
                }
    }
}