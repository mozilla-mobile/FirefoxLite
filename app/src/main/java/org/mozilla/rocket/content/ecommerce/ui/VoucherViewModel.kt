package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.ecommerce.domain.GetVouchersUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherKey
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.sha256
import org.mozilla.rocket.util.getJsonArray

class VoucherViewModel(
    private val getVouchers: GetVouchersUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _voucherItems = MutableLiveData<List<DelegateAdapter.UiModel>>()
    val voucherItems: LiveData<List<DelegateAdapter.UiModel>> = _voucherItems

    val openVoucher = SingleLiveEvent<OpenLinkAction>()

    var versionId = 0L

    fun onVoucherItemClicked(voucherItem: Voucher) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.SHOPPING,
            voucherItem.source,
            voucherItem.source,
            TelemetryWrapper.Extra_Value.SHOPPING_VOUCHER,
            voucherItem.url.sha256(),
            voucherItem.subCategoryId,
            versionId
        )
        openVoucher.value = OpenLinkAction(voucherItem.url, telemetryData)
    }

    fun requestVouchers() {
        launchDataLoad {
            val result = getVouchers()
            if (result is Result.Success) {
                versionId = System.currentTimeMillis()
                _voucherItems.postValue(result.data.jsonStringToVoucherItems())
            }
        }
    }

    private fun String.jsonStringToVoucherItems(): List<Voucher>? {
        return try {
            this.getJsonArray { createVoucherItem(it) }
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    private fun createVoucherItem(jsonObject: JSONObject): Voucher =
        Voucher(
            jsonObject.optString(VoucherKey.KEY_URL),
            jsonObject.optString(VoucherKey.KEY_NAME),
            jsonObject.optString(VoucherKey.KEY_IMAGE),
            jsonObject.optString(VoucherKey.KEY_SOURCE)
        )

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