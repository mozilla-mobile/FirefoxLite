package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class ShouldEnableTurboModeUseCase(val repository: ShoppingSearchRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldEnableTurboMode()
    }
}