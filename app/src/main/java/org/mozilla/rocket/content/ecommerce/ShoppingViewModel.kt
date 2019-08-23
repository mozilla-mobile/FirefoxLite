package org.mozilla.rocket.content.ecommerce

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.adapter.RunwayItem
import org.mozilla.rocket.content.ecommerce.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepo

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

    fun onRunwayItemClicked(context: Context, runwayItem: RunwayItem) {
        ScreenNavigator.get(context).showBrowserScreen(runwayItem.linkUrl, true, false)
    }

    fun onCouponItemClicked(context: Context, couponItem: Coupon) {
        ScreenNavigator.get(context).showBrowserScreen(couponItem.link.url, true, false)
//        TelemetryWrapper.clickOnPromoItem(
//                pos = position.toString(),
//                id = couponItem.id,
//                feed = couponItem.feed,
//                source = couponItem.link.source,
//                category = couponItem.category,
//                subcategory = couponItem.subcategory
//        )
    }

    fun onVoucherItemClicked(context: Context, voucherItem: Voucher) {
        ScreenNavigator.get(context).showBrowserScreen(voucherItem.url, true, false)
//        TelemetryWrapper.clickOnEcItem(
//                pos = position.toString(),
//                source = voucherItem.source,
//                category = voucherItem.name
//        )
    }
}