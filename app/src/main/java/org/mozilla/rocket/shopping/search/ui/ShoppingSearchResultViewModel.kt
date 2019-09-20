package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.shopping.search.domain.CheckContentSwitchOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CompleteContentSwitchOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSitesUseCase: GetShoppingSearchSitesUseCase,
    checkContentSwitchOnboardingFirstRunUseCase: CheckContentSwitchOnboardingFirstRunUseCase,
    completeContentSwitchOnboardingFirstRunUseCase: CompleteContentSwitchOnboardingFirstRunUseCase
) : ViewModel() {

    private val searchKeyword = MutableLiveData<String>()
    val shoppingSearchSites: LiveData<List<ShoppingSearchSite>> = searchKeyword.switchMap { getShoppingSearchSitesUseCase(it) }
    val goPreferences = SingleLiveEvent<Unit>()
    val showOnboardingDialog = SingleLiveEvent<Unit>()

    init {
        if (checkContentSwitchOnboardingFirstRunUseCase()) {
            showOnboardingDialog.call()
            completeContentSwitchOnboardingFirstRunUseCase()
        }
    }

    fun search(keyword: String) {
        searchKeyword.postValue(keyword)
    }
}
