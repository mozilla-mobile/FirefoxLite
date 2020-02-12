package org.mozilla.rocket.home.topsites.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.home.contenthub.domain.GetNonLiveDataContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.topsites.ui.Site

open class GetTopSitesWithContentItemUseCase(
    private val getTopSitesUseCase: GetTopSitesUseCase,
    private val getNonLiveDataContentHubItemsUseCase: GetNonLiveDataContentHubItemsUseCase
) {

    open suspend operator fun invoke(): List<Site> = withContext(Dispatchers.IO) {
        (getNonLiveDataContentHubItemsUseCase()?.toSites() ?: emptyList()) + getTopSitesUseCase(enableFixedSites = false)
    }
}

fun List<ContentHub.Item>.toSites(): List<Site> = this.map { it.toSite() }

fun ContentHub.Item.toSite(): Site = when (this) {
    is ContentHub.Item.Travel -> Site.ContentItem.Travel(iconResId, textResId, isUnread)
    is ContentHub.Item.Games -> Site.ContentItem.Games(iconResId, textResId, isUnread)
    is ContentHub.Item.News -> Site.ContentItem.News(iconResId, textResId, isUnread)
    is ContentHub.Item.Shopping -> Site.ContentItem.Shopping(iconResId, textResId, isUnread)
}