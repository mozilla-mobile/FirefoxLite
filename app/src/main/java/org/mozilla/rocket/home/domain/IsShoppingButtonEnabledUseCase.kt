package org.mozilla.rocket.home.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class IsShoppingButtonEnabledUseCase(private val shoppingSearchRepository: ShoppingSearchRepository) {

    operator fun invoke(): Boolean = shoppingSearchRepository.isShoppingSearchEnabled()
}