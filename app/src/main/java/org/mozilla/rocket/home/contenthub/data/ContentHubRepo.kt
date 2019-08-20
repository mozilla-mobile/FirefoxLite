package org.mozilla.rocket.home.contenthub.data

import org.mozilla.focus.R

class ContentHubRepo {

    fun getContentHubItems(): List<ContentHubItem> =
            listOf(
                ContentHubItem.Travel(R.drawable.ic_lock),
                ContentHubItem.Shopping(R.drawable.ic_lock),
                ContentHubItem.News(R.drawable.ic_lock),
                ContentHubItem.Games(R.drawable.ic_lock)
            )
}

sealed class ContentHubItem(open val iconResId: Int) {
    data class Travel(override val iconResId: Int) : ContentHubItem(iconResId)
    data class Shopping(override val iconResId: Int) : ContentHubItem(iconResId)
    data class News(override val iconResId: Int) : ContentHubItem(iconResId)
    data class Games(override val iconResId: Int) : ContentHubItem(iconResId)
}