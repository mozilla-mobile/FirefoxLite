package org.mozilla.rocket.content.ecommerce.ui

import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

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
                    getStringResourceId(subcategory.subcategoryId),
                    subcategory.subcategoryId,
                    subcategory.items.map { dealItem -> toProductItem(dealItem) }
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

    private fun toProductItem(item: ApiItem): ProductItem =
        ProductItem(
            item.sourceName,
            item.categoryName,
            item.subCategoryId,
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
                    item.categoryName,
                    item.subCategoryId,
                    item.image,
                    item.destination,
                    item.title,
                    item.componentId,
                    item.endDate.toRemainingDays()
                )
            }
    }

    private fun getStringResourceId(subCategoryId: Int): Int =
        when (subCategoryId) {
            1 -> R.string.shopping_deal_subcategory_1
            2 -> R.string.shopping_deal_subcategory_2
            else -> 0
        }

    private fun Long.toRemainingDays(): Long {
        val currentTime = System.currentTimeMillis()
        return when {
            this == 0L -> Long.MIN_VALUE
            this < currentTime -> 0L
            else -> ceil((this - currentTime).toDouble() / TimeUnit.DAYS.toMillis(1)).toLong()
        }
    }
}