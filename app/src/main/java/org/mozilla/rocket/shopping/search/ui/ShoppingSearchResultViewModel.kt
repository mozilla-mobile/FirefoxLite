package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSitesUseCase: GetShoppingSearchSitesUseCase
) : ViewModel() {

    private val _shoppingSearchSites = MutableLiveData<List<ShoppingSearchSite>>()
    val shoppingSearchSites: LiveData<List<ShoppingSearchSite>>
        get() = _shoppingSearchSites

    fun search(searchKeyword: String) {
        _shoppingSearchSites.value = getShoppingSearchSitesUseCase(searchKeyword)
    }
}