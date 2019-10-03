package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.data.ShoppingMapper
import org.mozilla.rocket.content.ecommerce.domain.GetDealsUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import org.mozilla.rocket.download.SingleLiveEvent

class DealViewModel(
    private val getDeals: GetDealsUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _dealItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = getDeals()
                if (result is Result.Success) {
                    value = ShoppingMapper.toDeals(result.data)
                }
            }
        }
    }
    val dealItems: LiveData<List<DelegateAdapter.UiModel>> = _dealItems

    val openProduct = SingleLiveEvent<String>()

    fun onProductItemClicked(productItem: ProductItem) {
        openProduct.value = productItem.linkUrl
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

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}