package org.mozilla.rocket.content.portal

import org.mozilla.focus.utils.AppConfigWrapper

/**
 * Storing the logic of how we provide content portal features
 *
 * */
class ContentFeature {

    companion object {
        const val TYPE_NEWS = 1 shl 0
        const val TYPE_TICKET = 1 shl 1
        const val TYPE_COUPON = 1 shl 2
        const val TYPE_KEY = "contentType"
        const val EXTRA_CONFIG_NEWS = "extra_config_news"
        const val SETTING_REQUEST_CODE = 1492
    }

    fun hasNews() = AppConfigWrapper.hasNewsPortal()

    fun hasCoupon() = AppConfigWrapper.hasEcommerceCoupons()

    fun hasShoppingLink() = AppConfigWrapper.hasEcommerceShoppingLink()

    fun hasContentPortal() = hasNews() || (hasCoupon() && hasShoppingLink())

    // get the eCommerceFeatures from remote config
    // if we want to change the order of tabs we should do it here.
    fun eCommerceFeatures(): ArrayList<Int> {

        val features = ArrayList<Int>()
        if (hasCoupon()) {
            features.add(TYPE_COUPON)
        }

        if (hasShoppingLink()) {
            features.add(TYPE_TICKET)
        }

        return features
    }
}