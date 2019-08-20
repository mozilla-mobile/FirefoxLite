package org.mozilla.rocket.home.contenthub.domain

import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.ui.ContentHub

class GetContentHubItemsUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(): List<ContentHub.Item> = contentHubRepo.getContentHubItems().toViewItem()
}

private fun List<ContentHubRepo.Item>.toViewItem(): List<ContentHub.Item> = map { it.toViewItem() }

private fun ContentHubRepo.Item.toViewItem(): ContentHub.Item = ContentHub.Item(iconResId)