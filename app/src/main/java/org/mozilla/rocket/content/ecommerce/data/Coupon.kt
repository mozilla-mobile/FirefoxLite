package org.mozilla.rocket.content.ecommerce.data

object CouponKey {
    const val KEY_ID = "id"
    const val KEY_CATEGORY = "category"
    const val KEY_SUBCATEGORY = "subcategory"
    const val KEY_FEED = "feed"
    const val KEY_START = "start"
    const val KEY_END = "end"
    const val KEY_ACTIVE = "active"
}

data class Coupon(
    val id: String,
    val category: String,
    val subcategory: String,
    val feed: String,
    val start: Long,
    val end: Long,
    val active: Boolean,
    val link: ShoppingLink
)