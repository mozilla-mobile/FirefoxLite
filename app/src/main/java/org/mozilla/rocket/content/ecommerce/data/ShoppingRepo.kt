package org.mozilla.rocket.content.ecommerce.data

import android.content.Context
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.CouponFragment
import org.mozilla.rocket.content.ecommerce.ui.DealFragment
import org.mozilla.rocket.content.ecommerce.ui.VoucherFragment
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherKey
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray

class ShoppingRepo(private val appContext: Context) {

    suspend fun getDeals(): List<DelegateAdapter.UiModel>? {
        return withContext(Dispatchers.IO) {
            getDealItems()
        }
    }

    suspend fun getCoupons(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getCouponItems()
        }
    }

    suspend fun getVouchers(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getVoucherItems() ?: emptyList()
        }
    }

    // TODO: remove mock data
    private fun getDealItems(): List<DelegateAdapter.UiModel> =
            ShoppingMapper.toDeals(DealEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.product_mock_items)))

    // TODO: remove mock data
    private fun getCouponItems(): List<DelegateAdapter.UiModel> =
            ShoppingMapper.toCoupons(DealEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.coupon_mock_items)))

    private fun getVoucherItems(): List<Voucher>? =
            FirebaseHelper.getFirebase().getRcString("str_e_commerce_shoppinglinks").jsonStringToVoucherItems()

    fun getShoppingTabItems(): List<ShoppingTabItem> =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.shopping_tab_items)
                ?.jsonStringToShoppingTabItems() ?: emptyList()
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

sealed class ShoppingTabItem(val fragment: Fragment, val titleResId: Int) {
    class DealTab : ShoppingTabItem(DealFragment(), R.string.shopping_vertical_category_1)
    class CouponTab : ShoppingTabItem(CouponFragment(), R.string.shopping_vertical_category_2)
    class VoucherTab : ShoppingTabItem(VoucherFragment(), R.string.shopping_vertical_category_3)
}

private fun createShoppingTabItem(type: Int): ShoppingTabItem =
        when (type) {
            1 -> ShoppingTabItem.DealTab()
            2 -> ShoppingTabItem.CouponTab()
            3 -> ShoppingTabItem.VoucherTab()
            else -> error("Unsupported shopping tab item type $type")
        }