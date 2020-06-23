package org.mozilla.rocket.home.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository

class IsHomeScreenShoppingButtonEnabledUseCase(private val shoppingSearchRepository: ShoppingSearchRepository) {

    operator fun invoke(): Boolean = shoppingSearchRepository.isHomeScreenShoppingSearchEnabled()
}