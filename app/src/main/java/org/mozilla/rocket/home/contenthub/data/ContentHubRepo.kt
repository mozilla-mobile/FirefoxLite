package org.mozilla.rocket.home.contenthub.data

import org.mozilla.focus.R

class ContentHubRepo {

    fun getContentHubItems(): List<Item> =
            listOf(
                Item(R.drawable.ic_lock),
                Item(R.drawable.ic_lock),
                Item(R.drawable.ic_lock),
                Item(R.drawable.ic_lock)
            )

    data class Item(val iconResId: Int)
}