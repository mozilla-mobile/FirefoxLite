package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class SetSearchInputOnboardingIsShownUseCase(val repository: ShoppingSearchRepository) {
    operator fun invoke() {
        repository.setSearchInputOnboardingIsShown()
    }
}