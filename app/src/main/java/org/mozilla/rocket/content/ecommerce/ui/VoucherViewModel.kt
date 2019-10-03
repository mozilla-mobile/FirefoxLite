package org.mozilla.rocket.content.ecommerce.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.domain.GetVouchersUseCase
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherKey
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.toJsonArray

class VoucherViewModel(
    private val getVouchers: GetVouchersUseCase
) : ViewModel() {

    private val _isDataLoading = MutableLiveData<State>()
    val isDataLoading: LiveData<State> = _isDataLoading

    private val _voucherItems by lazy {
        MutableLiveData<List<DelegateAdapter.UiModel>>().apply {
            launchDataLoad {
                val result = getVouchers()
                if (result is Result.Success) {
                    value = result.data.jsonStringToVoucherItems()
                }
            }
        }
    }
    val voucherItems: LiveData<List<DelegateAdapter.UiModel>> = _voucherItems

    val openVoucher = SingleLiveEvent<String>()

    fun onVoucherItemClicked(voucherItem: Voucher) {
        openVoucher.value = voucherItem.url
    }

    private fun String.jsonStringToVoucherItems(): List<Voucher>? {
        return try {
            val jsonArray = this.toJsonArray()
            (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createVoucherItem(jsonObject) }
                .shuffled()
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

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}