package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSite
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class UpdateShoppingSitesUseCase(private val repository: ShoppingSearchRepository) {

    operator fun invoke(shoppingSiteItems: List<ShoppingSiteItem>) {
        repository.updateShoppingSites(shoppingSiteItems.toShoppingSites())
    }
}

private fun List<ShoppingSiteItem>.toShoppingSites(): List<ShoppingSite> =
        map { it.toShoppingSite() }

private fun ShoppingSiteItem.toShoppingSite(): ShoppingSite =
        ShoppingSite(
            title,
            searchUrl,
            displayUrl,
            showPrompt,
            isChecked
        )