package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class ShouldShowShoppingSearchOnboardingUseCase(
    private val shoppingSearchRepository: ShoppingSearchRepository,
    private val contentPrefRepo: ContentPrefRepo,
    private val newFeatureNotice: NewFeatureNotice
) {
    operator fun invoke(): Boolean {
        val remoteConfig = shoppingSearchRepository.getHomeShoppingSearchEnabledGroups()
                ?.find { it.groupId == contentPrefRepo.getContentPref().id }?.isEnabled
        val localConfig = contentPrefRepo.getContentPref() is ContentPrefRepo.ContentPref.Shopping
        return (remoteConfig ?: localConfig) && !newFeatureNotice.hasHomeShoppingSearchOnboardingShown()
    }
}