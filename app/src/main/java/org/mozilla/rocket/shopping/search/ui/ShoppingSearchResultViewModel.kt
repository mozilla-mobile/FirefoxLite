package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase.ShoppingSearchSite
import org.mozilla.rocket.shopping.search.domain.SetSearchResultOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldEnableTurboModeUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchResultOnboardingUseCase

class ShoppingSearchResultViewModel(
    private val getShoppingSearchSites: GetShoppingSearchSitesUseCase,
    shouldEnableTurboMode: ShouldEnableTurboModeUseCase,
    shouldShowSearchResultOnboarding: ShouldShowSearchResultOnboardingUseCase,
    setSearchResultOnboardingIsShown: SetSearchResultOnboardingIsShownUseCase
) : ViewModel() {

    private val searchKeyword = MutableLiveData<String>()
    private val shoppingSearchSites: LiveData<List<ShoppingSearchSite>> = searchKeyword.switchMap { getShoppingSearchSites(it) }
    val goPreferences = SingleLiveEvent<Unit>()
    val showOnboardingDialog = SingleLiveEvent<Unit>()
    val goBackToInputPage = SingleLiveEvent<Unit>()

    val uiModel = MediatorLiveData<ShoppingSearchResultUiModel>().apply {
        addSource(shoppingSearchSites) {
            val newUiModel = ShoppingSearchResultUiModel(it, shouldEnableTurboMode())
            if (value == newUiModel) {
                return@addSource
            }
            value = newUiModel
        }
    }

    init {
        if (shouldShowSearchResultOnboarding()) {
            showOnboardingDialog.call()
            setSearchResultOnboardingIsShown()
        }
    }

    fun search(keyword: String) {
        searchKeyword.postValue(keyword)
    }

    fun onUrlBarClicked() {
        goBackToInputPage.call()
        TelemetryWrapper.clickUrlbar(TelemetryWrapper.Extra_Value.SHOPPING)
    }

    fun onShoppingSearchButtonClick() {
        goBackToInputPage.call()
        TelemetryWrapper.clickToolbarTabSwipe(TelemetryWrapper.Extra_Value.SHOPPING, TelemetryWrapper.Extra_Value.TAB_SWIPE)
    }

    data class ShoppingSearchResultUiModel(
        val shoppingSearchSiteList: List<ShoppingSearchSite>,
        val shouldEnableTurboMode: Boolean
    )
}
