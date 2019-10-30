package org.mozilla.rocket.shopping.search.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSite
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class GetShoppingSitesUseCase(val repository: ShoppingSearchRepository) {

    operator fun invoke(): LiveData<List<ShoppingSiteItem>> =
        repository.getShoppingSitesData()
            .map {
                it.toShoppingSiteItems().apply {
                    val allowSwitchOff = count { item -> item.isChecked } > MIN_SHOPPING_SITE_COUNT
                    forEach { item ->
                        item.isEnabled = !item.isChecked || allowSwitchOff
                    }
                }
            }

    companion object {
        private const val MIN_SHOPPING_SITE_COUNT = 2
    }
}

private fun List<ShoppingSite>.toShoppingSiteItems(): List<ShoppingSiteItem> =
    map { it.toShoppingSiteItem() }

private fun ShoppingSite.toShoppingSiteItem(): ShoppingSiteItem =
    ShoppingSiteItem(
        title,
        searchUrl,
        displayUrl,
        showPrompt,
        isChecked = isEnabled
    )