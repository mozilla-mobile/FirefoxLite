package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.domain.GetCouponsUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.download.SingleLiveEvent

class CouponViewModel(
    private val getCoupons: GetCouponsUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _couponItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = getCoupons()
                if (result is Result.Success) {
                    value = ShoppingMapper.toCoupons(result.data)
                } else if (result is Result.Error) {
                    throw (result.exception)
                }
            }
        }
    }

    val couponItems: LiveData<List<DelegateAdapter.UiModel>> = _couponItems

    val openCoupon = SingleLiveEvent<String>()

    fun onCouponItemClicked(couponItem: Coupon) {
        openCoupon.value = couponItem.linkUrl
    }

    fun onRetryButtonClicked() {
        launchDataLoad {
            val result = getCoupons()
            if (result is Result.Success) {
                _couponItems.postValue(ShoppingMapper.toCoupons(result.data))
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

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}