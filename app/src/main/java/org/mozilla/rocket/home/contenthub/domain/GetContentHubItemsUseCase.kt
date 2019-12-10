package org.mozilla.rocket.home.contenthub.domain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.mozilla.rocket.extension.combineLatest
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.contenthub.data.ContentHubItem
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.ui.ContentHub

class GetContentHubItemsUseCase(private val contentHubRepo: ContentHubRepo) {

    operator fun invoke(): LiveData<List<ContentHub.Item>> {
        val defaultItemsLiveData = MutableLiveData<List<ContentHubItem>>().apply {
            value = contentHubRepo.getDefaultContentHubItems()
        }
        val configuredItemsLiveData = contentHubRepo.getConfiguredContentHubItemsLive()
        return combineLatest(defaultItemsLiveData, configuredItemsLiveData)
                .map { (defaultItems, configuredItems) ->
                    configuredItems ?: defaultItems
                }
                .map { it.toViewItem() }
    }
}

private fun List<ContentHubItem>.toViewItem(): List<ContentHub.Item> = map { it.toViewItem() }

private fun ContentHubItem.toViewItem(): ContentHub.Item = when (this) {
    is ContentHubItem.Travel -> ContentHub.Item.Travel(iconResId, textResId, isUnread)
    is ContentHubItem.Shopping -> ContentHub.Item.Shopping(iconResId, textResId, isUnread)
    is ContentHubItem.News -> ContentHub.Item.News(iconResId, textResId, isUnread)
    is ContentHubItem.Games -> ContentHub.Item.Games(iconResId, textResId, isUnread)
}
