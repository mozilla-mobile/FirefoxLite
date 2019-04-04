package org.mozilla.rocket.content.data

data class Ticket(val url: String, val name: String, val image: String)

object TicketKey {
    const val KEY_NAME = "name"
    const val KEY_URL = "url"
    const val KEY_IMAGE = "img"
}
