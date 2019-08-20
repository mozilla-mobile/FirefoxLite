package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.rocket.home.contenthub.data.ContentHubItem
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.ui.ContentHub

class GetContentHubItemsUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(): List<ContentHub.Item> = contentHubRepo.getContentHubItems().toViewItem()
}

private fun List<ContentHubItem>.toViewItem(): List<ContentHub.Item> = map { it.toViewItem() }

private fun ContentHubItem.toViewItem(): ContentHub.Item = when (this) {
    is ContentHubItem.Travel -> ContentHub.Item.Travel(iconResId)
    is ContentHubItem.Shopping -> ContentHub.Item.Shopping(iconResId)
    is ContentHubItem.News -> ContentHub.Item.News(iconResId)
    is ContentHubItem.Games -> ContentHub.Item.Games(iconResId)
}
