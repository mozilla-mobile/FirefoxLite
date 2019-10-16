package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite
import org.mozilla.rocket.shopping.search.domain.SetSearchResultOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchResultOnboardingUseCase

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSites: GetShoppingSearchSitesUseCase,
    shouldShowSearchResultOnboarding: ShouldShowSearchResultOnboardingUseCase,
    setSearchResultOnboardingIsShown: SetSearchResultOnboardingIsShownUseCase
) : ViewModel() {

    private val searchKeyword = MutableLiveData<String>()
    val shoppingSearchSites: LiveData<List<ShoppingSearchSite>> = searchKeyword.switchMap { getShoppingSearchSites(it) }
    val goPreferences = SingleLiveEvent<Unit>()
    val showOnboardingDialog = SingleLiveEvent<Unit>()

    init {
        if (shouldShowSearchResultOnboarding()) {
            showOnboardingDialog.call()
            setSearchResultOnboardingIsShown()
        }
    }

    fun search(keyword: String) {
        searchKeyword.postValue(keyword)
    }
}
