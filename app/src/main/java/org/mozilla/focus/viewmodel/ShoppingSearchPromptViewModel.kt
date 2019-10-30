package org.mozilla.focus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class ShoppingSearchPromptViewModel(getShoppingSite: GetShoppingSitesUseCase) : ViewModel() {
    val promptVisibilityState = MutableLiveData<VisibilityState>()
    val matchedShoppingSiteTitle = MutableLiveData<String?>()
    val shoppingSiteList: LiveData<List<ShoppingSiteItem>> = getShoppingSite()

    val openShoppingSearch = SingleLiveEvent<Unit>()

    fun checkShoppingSearchPromptVisibility(url: String?, forwardSiteUrl: String?, previousSiteUrl: String?) {
        val currentShoppingSiteTitle: String? = getPromptShoppingSiteTitle(url)
        matchedShoppingSiteTitle.value = currentShoppingSiteTitle

        val isShoppingSite = (matchedShoppingSiteTitle.value != null)
        if (isShoppingSite) {
            if (!currentShoppingSiteTitle.isNullOrEmpty()) {
                var hasForwardUrl = false
                if (!forwardSiteUrl.isNullOrEmpty()) {
                    hasForwardUrl = true
                    val forwardShoppingSiteTitle: String? = getPromptShoppingSiteTitle(forwardSiteUrl)
                    if (currentShoppingSiteTitle.equals(forwardShoppingSiteTitle, true)) {
                        promptVisibilityState.value = VisibilityState.Collapsed
                        return
                    }
                }

                if (!hasForwardUrl) {
                    if (!previousSiteUrl.isNullOrEmpty()) {
                        val previousShoppingSiteTitle: String? = getPromptShoppingSiteTitle(previousSiteUrl)
                        if (currentShoppingSiteTitle.equals(previousShoppingSiteTitle, true)) {
                            promptVisibilityState.value = VisibilityState.Collapsed
                            return
                        }
                    }
                }
            }

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

    sealed class VisibilityState {
        object Hidden : VisibilityState()
        object Expanded : VisibilityState()
        object Collapsed : VisibilityState()
    }
}