package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.RunwayItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepo
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import org.mozilla.rocket.download.SingleLiveEvent

class ShoppingViewModel(
    private val shoppingRepo: ShoppingRepo
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _dealItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                value = shoppingRepo.getDeals()
            }
        }
    }
    val dealItems: LiveData<List<DelegateAdapter.UiModel>> = _dealItems

    private val _couponItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                value = shoppingRepo.getCoupons()
            }
        }
    }
    val couponItems: LiveData<List<DelegateAdapter.UiModel>> = _couponItems

    private val _voucherItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                value = shoppingRepo.getVouchers()
            }
        }
    }
    val voucherItems: LiveData<List<DelegateAdapter.UiModel>> = _voucherItems

    val openRunway = SingleLiveEvent<String>()
    val openProduct = SingleLiveEvent<String>()
    val openCoupon = SingleLiveEvent<String>()
    val openVoucher = SingleLiveEvent<String>()

    fun onRunwayItemClicked(runwayItem: RunwayItem) {
        openRunway.value = runwayItem.linkUrl
    }

    fun onProductItemClicked(productItem: ProductItem) {
        openProduct.value = productItem.linkUrl
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