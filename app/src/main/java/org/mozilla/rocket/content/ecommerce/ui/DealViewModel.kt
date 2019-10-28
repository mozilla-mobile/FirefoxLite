package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.ecommerce.domain.GetDealsUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import org.mozilla.rocket.download.SingleLiveEvent

class DealViewModel(
    private val getDeals: GetDealsUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _dealItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val dealItems: LiveData<List<DelegateAdapter.UiModel>> = _dealItems

    val openProduct = SingleLiveEvent<OpenLinkAction>()

    var versionId = 0L

    fun onProductItemClicked(productItem: ProductItem) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.SHOPPING,
            productItem.source,
            productItem.source,
            TelemetryWrapper.Extra_Value.SHOPPING_DEAL,
            productItem.componentId,
            productItem.subCategoryId,
            versionId
        )
        openProduct.value = OpenLinkAction(productItem.linkUrl, telemetryData)
    }

    fun requestDeals() {
        getDealsUiModelList()
    }

    fun onRetryButtonClicked() {
        getDealsUiModelList()
    }

    fun getDealsUiModelList() {
        launchDataLoad {
            val result = getDeals()
            if (result is Result.Success) {
                versionId = result.data.version
                _dealItems.postValue(ShoppingMapper.toDeals(result.data))
            } else if (result is Result.Error) {
                throw (result.exception)
            }
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _isDataLoading.value = State.Loading
                block()
                _isDataLoading.value = State.Idle
            } catch (t: Throwable) {
                _isDataLoading.value = State.Error(t)
            }
        }
    }

    data class OpenLinkAction(val url: String, val telemetryData: ContentTabTelemetryData)

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}