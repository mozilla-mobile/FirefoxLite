package org.mozilla.rocket.content.ecommerce

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.adapter.RunwayItem
import org.mozilla.rocket.content.ecommerce.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepo
import org.mozilla.rocket.download.SingleLiveEvent

class ShoppingViewModel(
    private val shoppingRepo: ShoppingRepo
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _couponItems by lazy {
        val liveData = MutableLiveData<List<DelegateAdapter.UiModel>>()
        launchDataLoad {
            liveData.value = shoppingRepo.getCoupons()
        }
        return@lazy liveData
    }
    val couponItems: LiveData<List<DelegateAdapter.UiModel>> = _couponItems

    private val _voucherItems by lazy {
        val liveData = MutableLiveData<List<DelegateAdapter.UiModel>>()
        launchDataLoad {
            liveData.value = shoppingRepo.getVouchers()
        }
        return@lazy liveData
    }
    val voucherItems: LiveData<List<DelegateAdapter.UiModel>> = _voucherItems

    val openRunway = SingleLiveEvent<String>()
    val openCoupon = SingleLiveEvent<String>()
    val openVoucher = SingleLiveEvent<String>()

    fun onRunwayItemClicked(runwayItem: RunwayItem) {
        openRunway.value = runwayItem.linkUrl
    }

    fun onCouponItemClicked(couponItem: Coupon) {
        openCoupon.value = couponItem.link.url
    }

    fun onVoucherItemClicked(voucherItem: Voucher) {
        openVoucher.value = voucherItem.url
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