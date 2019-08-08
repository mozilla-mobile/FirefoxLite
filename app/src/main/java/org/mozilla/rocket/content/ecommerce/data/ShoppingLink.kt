package org.mozilla.rocket.content.ecommerce.data

object ShoppingLinkKey {
    const val KEY_NAME = "name"
    const val KEY_URL = "url"
    const val KEY_IMAGE = "img"
    const val KEY_SOURCE = "source"
}

data class ShoppingLink(
    val url: String,
    val name: String,
    val image: String,
    val source: String
)