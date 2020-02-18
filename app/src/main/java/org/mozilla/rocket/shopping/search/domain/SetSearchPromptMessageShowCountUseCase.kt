package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class SetSearchPromptMessageShowCountUseCase(val repository: ShoppingSearchRepository) {
    operator fun invoke(count: Int) {
        return repository.setSearchPromptMessageShowCount(count)
    }
}