package org.mozilla.rocket.content.data

object CouponKey {
    const val KEY_START = "start"
    const val KEY_END = "end"
    const val KEY_ACTIVE = "active"
}

data class Coupon(val link: ShoppingLink, val start: Long, val end: Long, val active: Boolean)