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
    val isUrlShoppingSite = MutableLiveData<Boolean>()
    val matchedShoppingSiteTitle = MutableLiveData<String?>()
    val shoppingSiteList: LiveData<List<ShoppingSiteItem>> = getShoppingSiteUseCase()

    val openShoppingSearch = SingleLiveEvent<Unit>()

    fun checkIsShoppingSite(url: String?) {
        matchedShoppingSiteTitle.value = shoppingSiteList.value?.filter { site -> url?.contains(site.displayUrl, true) == true }?.map { it.title }?.firstOrNull()
        isUrlShoppingSite.value = matchedShoppingSiteTitle.value != null
    }
}