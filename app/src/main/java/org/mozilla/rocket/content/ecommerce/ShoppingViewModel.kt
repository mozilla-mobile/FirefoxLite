package org.mozilla.rocket.content.ecommerce

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _isDataLoading = MutableLiveData<Boolean>()
    val isDataLoading: LiveData<Boolean> = _isDataLoading

    private val _couponItems by lazy {
        val liveData = MutableLiveData<List<DelegateAdapter.UiModel>>()
        _isDataLoading.value = true
        viewModelScope.launch {
            liveData.value = shoppingRepo.getCoupons()
            _isDataLoading.value = false
        }
        return@lazy liveData
    }
    val couponItems: LiveData<List<DelegateAdapter.UiModel>> = _couponItems

    private val _voucherItems by lazy {
        val liveData = MutableLiveData<List<DelegateAdapter.UiModel>>()
        _isDataLoading.value = true
        viewModelScope.launch {
            liveData.value = shoppingRepo.getVouchers()
            _isDataLoading.value = false
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
}