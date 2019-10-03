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
            1 -> ShoppingTabItem.DealTab()
            2 -> ShoppingTabItem.CouponTab()
            3 -> ShoppingTabItem.VoucherTab()
            else -> error("Unsupported shopping tab item type $type")
        }

    sealed class ShoppingTabItem(val fragment: Fragment, val titleResId: Int) {
        class DealTab : ShoppingTabItem(DealFragment(), R.string.shopping_vertical_category_1)
        class CouponTab : ShoppingTabItem(CouponFragment(), R.string.shopping_vertical_category_2)
        class VoucherTab : ShoppingTabItem(VoucherFragment(), R.string.shopping_vertical_category_3)
    }
}