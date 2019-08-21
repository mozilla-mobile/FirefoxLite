package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository.Site
import org.mozilla.rocket.shopping.search.domain.SearchShoppingSiteUseCase

class ShoppingSearchResultViewModel(
    private val searchShoppingSite: SearchShoppingSiteUseCase
) : ViewModel() {

    private val _uiModel = MutableLiveData<ShoppingSearchResultUiModel>()
    val uiModel: LiveData<ShoppingSearchResultUiModel>
        get() = _uiModel

    fun search(searchKeyword: String) = viewModelScope.launch(Dispatchers.Default) {
        val searchShoppingSiteResult = searchShoppingSite(searchKeyword)
        if (searchShoppingSiteResult is Result.Success) {
            withContext(Dispatchers.Main) { emitUiModel(searchShoppingSiteResult.data) }
        }
    }

    private fun emitUiModel(sites: List<Site>) {
        _uiModel.value = ShoppingSearchResultUiModel(sites)
    }
}

data class ShoppingSearchResultUiModel(
    val sites: List<Site>
)