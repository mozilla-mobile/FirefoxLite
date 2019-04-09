package org.mozilla.rocket.content.data

data class ShoppingLink(val url: String, val name: String, val image: String, val source: String)

object ShoppingLinkKey {
    const val KEY_NAME = "name"
    const val KEY_URL = "url"
    const val KEY_IMAGE = "img"
    const val KEY_SOURCE = "source"
}
