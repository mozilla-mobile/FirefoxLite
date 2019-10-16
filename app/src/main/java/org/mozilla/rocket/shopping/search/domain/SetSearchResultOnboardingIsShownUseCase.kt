package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class SetSearchResultOnboardingIsShownUseCase(val repository: ShoppingSearchRepository) {
    operator fun invoke() {
        repository.setSearchResultOnboardingIsShown()
    }
}