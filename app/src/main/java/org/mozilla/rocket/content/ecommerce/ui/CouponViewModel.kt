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
import org.mozilla.rocket.content.ecommerce.domain.GetCouponsUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.download.SingleLiveEvent

class CouponViewModel(
    private val getCoupons: GetCouponsUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _couponItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val couponItems: LiveData<List<DelegateAdapter.UiModel>> = _couponItems

    val openCoupon = SingleLiveEvent<OpenLinkAction>()

    var versionId = 0L

    fun onCouponItemClicked(couponItem: Coupon) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.SHOPPING,
            couponItem.source,
            couponItem.source,
            TelemetryWrapper.Extra_Value.SHOPPING_COUPON,
            couponItem.componentId,
            couponItem.subCategoryId,
            versionId
        )
        openCoupon.value = OpenLinkAction(couponItem.linkUrl, telemetryData)
    }

    fun requestCoupons() {
        getCouponUiModelList()
    }

    fun onRetryButtonClicked() {
        getCouponUiModelList()
    }

    fun getCouponUiModelList() {
        launchDataLoad {
            val result = getCoupons()
            if (result is Result.Success) {
                versionId = result.data.version
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

    data class OpenLinkAction(val url: String, val telemetryData: ContentTabTelemetryData)

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}