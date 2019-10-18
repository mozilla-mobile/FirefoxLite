package org.mozilla.rocket.content.ecommerce.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.mozilla.focus.R
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.domain.GetShoppingTabItemsUseCase
import org.mozilla.rocket.util.toJsonArray

class ShoppingViewModel(
    private val getShoppingTabItems: GetShoppingTabItemsUseCase
) : ViewModel() {

    private val _shoppingTabItems by lazy {
        MutableLiveData<List<ShoppingTabItem>>().apply {
            viewModelScope.launch {
                val result = getShoppingTabItems()
                if (result is Result.Success) {
                    value = result.data.jsonStringToShoppingTabItems()
                }
            }
        }
    }
    val shoppingTabItems: LiveData<List<ShoppingTabItem>> = _shoppingTabItems

    fun refresh() {
        _shoppingTabItems.value = emptyList()

        viewModelScope.launch {
            val result = getShoppingTabItems()
            if (result is Result.Success) {
                _shoppingTabItems.value = result.data.jsonStringToShoppingTabItems()
            }
        }
    }

    private fun String.jsonStringToShoppingTabItems(): List<ShoppingTabItem>? {
        return try {
            val jsonArray = this.toJsonArray()
            (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> jsonObject.getInt("type") }
                .map { type -> createShoppingTabItem(type) }
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    private fun createShoppingTabItem(type: Int): ShoppingTabItem =
        when (type) {
            ShoppingTabItem.TYPE_DEAL_TAB -> ShoppingTabItem(ShoppingTabItem.TYPE_DEAL_TAB, R.string.shopping_vertical_category_1)
            ShoppingTabItem.TYPE_COUPON_TAB -> ShoppingTabItem(ShoppingTabItem.TYPE_COUPON_TAB, R.string.shopping_vertical_category_2)
            ShoppingTabItem.TYPE_VOUCHER_TAB -> ShoppingTabItem(ShoppingTabItem.TYPE_VOUCHER_TAB, R.string.shopping_vertical_category_3)
            else -> error("Unsupported shopping tab item type $type")
        }

    data class ShoppingTabItem(
        val type: Int,
        val titleResId: Int
    ) {
        fun createFragment(): Fragment {
            return when (type) {
                TYPE_DEAL_TAB -> DealFragment()
                TYPE_COUPON_TAB -> CouponFragment()
                TYPE_VOUCHER_TAB -> VoucherFragment()
                else -> error("Unsupported shopping tab item type $type")
            }
        }

        companion object {
            const val TYPE_DEAL_TAB = 0
            const val TYPE_COUPON_TAB = 1
            const val TYPE_VOUCHER_TAB = 2
        }
    }
}