package org.mozilla.rocket.home.onboarding.domain

import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class ShouldShowShoppingSearchOnboardingUseCase(
    private val shoppingSearchRepository: ShoppingSearchRepository,
    private val newFeatureNotice: NewFeatureNotice
) {
    operator fun invoke(): Boolean {
        return shoppingSearchRepository.isShoppingSearchEnabled() && !newFeatureNotice.hasHomeShoppingSearchOnboardingShown()
    }
}