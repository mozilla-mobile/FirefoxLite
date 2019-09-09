package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository

class CheckOnboardingFirstRunUseCase(val repository: ShoppingSearchSiteRepository) {
    operator fun invoke(): Boolean {
        return repository.getOnboardingPref()
    }
}