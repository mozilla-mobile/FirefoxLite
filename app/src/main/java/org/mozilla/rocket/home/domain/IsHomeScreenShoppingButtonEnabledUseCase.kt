package org.mozilla.rocket.home.domain

import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class IsHomeScreenShoppingButtonEnabledUseCase(
    private val shoppingSearchRepository: ShoppingSearchRepository,
    private val contentPrefRepo: ContentPrefRepo
) {

    operator fun invoke(): Boolean {
        val remoteConfig = shoppingSearchRepository.getHomeShoppingSearchEnabledGroups()
                ?.find { it.groupId == contentPrefRepo.getContentPref().id }?.isEnabled
        val localConfig = contentPrefRepo.getContentPref() is ContentPrefRepo.ContentPref.Shopping
        return remoteConfig ?: localConfig
    }
}