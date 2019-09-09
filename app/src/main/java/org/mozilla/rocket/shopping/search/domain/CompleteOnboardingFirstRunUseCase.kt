package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository

class CompleteOnboardingFirstRunUseCase(val repository: ShoppingSearchSiteRepository) {
    operator fun invoke() {
        repository.setOnboardingPref(false)
    }
}