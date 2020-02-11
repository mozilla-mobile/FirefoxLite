package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.rocket.home.contenthub.data.ContentHubItem
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.ui.ContentHub

class GetNonLiveDataContentHubItemsUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(): List<ContentHub.Item>? {
        val defaultItems = contentHubRepo.getDefaultContentHubItems()
        val configuredItems = contentHubRepo.getConfiguredContentHubItems()
        return (configuredItems ?: defaultItems)?.toViewItem()
    }
}

private fun List<ContentHubItem>.toViewItem(): List<ContentHub.Item> = map { it.toViewItem() }

private fun ContentHubItem.toViewItem(): ContentHub.Item = when (this) {
    is ContentHubItem.Travel -> ContentHub.Item.Travel(iconResId, textResId, isUnread)
    is ContentHubItem.Shopping -> ContentHub.Item.Shopping(iconResId, textResId, isUnread)
    is ContentHubItem.News -> ContentHub.Item.News(iconResId, textResId, isUnread)
    is ContentHubItem.Games -> ContentHub.Item.Games(iconResId, textResId, isUnread)
}
