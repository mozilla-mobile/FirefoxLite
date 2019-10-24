package org.mozilla.focus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class ShoppingSearchPromptViewModel(
    private val getShoppingSiteUseCase: GetShoppingSitesUseCase
) : ViewModel() {
    val promptVisibilityState = MutableLiveData<VisibilityState>()
    val matchedShoppingSiteTitle = MutableLiveData<String?>()
    val shoppingSiteList: LiveData<List<ShoppingSiteItem>> = getShoppingSiteUseCase()

    val openShoppingSearch = SingleLiveEvent<Unit>()

    fun checkShoppingSearchPromptVisibility(url: String?, forwardSiteUrl: String?, previousSiteUrl: String?) {
        val currentShoppingSiteTitle: String? = getShoppingSiteTitle(url)
        matchedShoppingSiteTitle.value = currentShoppingSiteTitle

        val isShoppingSite: Boolean = (matchedShoppingSiteTitle.value != null)
        if (isShoppingSite) {
            if (currentShoppingSiteTitle != null && currentShoppingSiteTitle.isNotEmpty()) {
                var hasForwardUrl = false
                if (forwardSiteUrl != null && forwardSiteUrl.isNotEmpty()) {
                    hasForwardUrl = true
                    val forwardShoppingSiteTitle: String? = getShoppingSiteTitle(forwardSiteUrl)
                    if (forwardShoppingSiteTitle != null && forwardShoppingSiteTitle.equals(currentShoppingSiteTitle, true)) {
                        promptVisibilityState.value = VisibilityState.Collapsed
                        return
                    }
                }

                if (!hasForwardUrl) {
                    if (previousSiteUrl != null && previousSiteUrl.isNotEmpty()) {
                        val previousShoppingSiteTitle: String? = getShoppingSiteTitle(previousSiteUrl)
                        if (previousShoppingSiteTitle != null && previousShoppingSiteTitle.equals(currentShoppingSiteTitle, true)) {
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

    private fun getShoppingSiteTitle(url: String?): String? {
        return shoppingSiteList.value?.filter { site -> url?.contains(site.displayUrl, true) == true }?.map { it.title }?.firstOrNull()
    }

    sealed class VisibilityState {
        object Hidden : VisibilityState()
        object Expanded : VisibilityState()
        object Collapsed : VisibilityState()
    }
}