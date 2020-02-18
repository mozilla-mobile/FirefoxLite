package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class GetSearchPromptMessageShowCountUseCase(val repository: ShoppingSearchRepository) {
    operator fun invoke(): Int {
        return repository.getSearchPromptMessageShowCount()
    }
}