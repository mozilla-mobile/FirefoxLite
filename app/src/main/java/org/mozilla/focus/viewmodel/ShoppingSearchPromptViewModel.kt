package org.mozilla.focus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class ShoppingSearchPromptViewModel(getShoppingSite: GetShoppingSitesUseCase) : ViewModel() {
    private val matchedShoppingSiteTitle = MutableLiveData<String?>()

    val promptVisibilityState = MutableLiveData<VisibilityState>()
    val shoppingSiteList: LiveData<List<ShoppingSiteItem>> = getShoppingSite()

    val openShoppingSearch = SingleLiveEvent<Unit>()

    fun onShoppingSearchPromptButtonClicked() {
        openShoppingSearch.call()

        matchedShoppingSiteTitle.value?.let { feed ->
            TelemetryWrapper.clickTabSwipeDrawer(TelemetryWrapper.Extra_Value.SHOPPING, feed)
        }
    }

    fun checkShoppingSearchPromptVisibility(url: String?) {
        val currentShoppingSiteTitle: String? = getPromptShoppingSiteTitle(url)
        matchedShoppingSiteTitle.value = currentShoppingSiteTitle

        val isShoppingSite = (matchedShoppingSiteTitle.value != null)
        if (isShoppingSite) {
            promptVisibilityState.value = VisibilityState.Expanded
        } else {
            promptVisibilityState.value = VisibilityState.Hidden
        }
    }

    private fun getPromptShoppingSiteTitle(url: String?): String? {
        return shoppingSiteList.value
            ?.filter { site -> site.showPrompt }
            ?.filter { site -> url?.contains(site.displayUrl, true) == true }
            ?.map { it.title }
            ?.firstOrNull()
    }

    fun onPromptIsShown() {
        matchedShoppingSiteTitle.value?.let { feed ->
            TelemetryWrapper.showTabSwipeDrawer(TelemetryWrapper.Extra_Value.SHOPPING, feed)
        }
    }

    sealed class VisibilityState {
        object Hidden : VisibilityState()
        object Expanded : VisibilityState()
    }
}