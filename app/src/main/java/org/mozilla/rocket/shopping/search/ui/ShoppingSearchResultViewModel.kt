package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.CheckOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CompleteOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSitesUseCase: GetShoppingSearchSitesUseCase,
    checkOnboardingFirstRunUseCase: CheckOnboardingFirstRunUseCase,
    completeOnboardingFirstRunUseCase: CompleteOnboardingFirstRunUseCase
) : ViewModel() {

    private val _shoppingSearchSites = MutableLiveData<List<ShoppingSearchSite>>()
    val shoppingSearchSites: LiveData<List<ShoppingSearchSite>>
        get() = _shoppingSearchSites
    val goPreferences = SingleLiveEvent<Unit>()
    val showOnboardingDialog = SingleLiveEvent<Unit> ()

    init {
        if (checkOnboardingFirstRunUseCase()) {
            showOnboardingDialog.call()
            completeOnboardingFirstRunUseCase()
        }
    }

    fun search(searchKeyword: String) {
        _shoppingSearchSites.value = getShoppingSearchSitesUseCase(searchKeyword)
    }
}