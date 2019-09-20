package org.mozilla.rocket.content.ecommerce.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherKey
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray
import java.util.UUID

class ShoppingRepo(private val appContext: Context) {

    suspend fun getDeals(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            listOf(
                    Runway(
                            getMockRunwayItems() ?: emptyList()
                    ),
                ProductCategory(UUID.randomUUID().toString(),
                    "Top Rated",
                    getMockProductItems()?.subList(0, 15) ?: emptyList()
                ),
                ProductCategory(UUID.randomUUID().toString(),
                    "Best Sellers",
                    getMockProductItems()?.subList(16, 30) ?: emptyList()
                )
            )
        }
    }

    suspend fun getCoupons(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getMockCouponItems() ?: emptyList()
        }
    }

    suspend fun getVouchers(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            getVoucherItems() ?: emptyList()
        }
    }

    // TODO: remove mock data
    private fun getMockRunwayItems(): List<RunwayItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.runway_mock_items)
                ?.jsonStringToRunwayItems()

    // TODO: remove mock data
    private fun getMockProductItems(): List<ProductItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.product_mock_items)
                ?.jsonStringToProductItems()

    // TODO: remove mock data
    private fun getMockCouponItems(): List<Coupon>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.coupon_mock_items)
                ?.jsonStringToCouponItems()

    private fun getVoucherItems(): List<Voucher>? =
            FirebaseHelper.getFirebase().getRcString("str_e_commerce_shoppinglinks").jsonStringToVoucherItems()
}

private fun String.jsonStringToRunwayItems(): List<RunwayItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createRunwayItem(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createRunwayItem(jsonObject: JSONObject): RunwayItem =
        RunwayItem(
                jsonObject.optInt("id"),
                jsonObject.optString("image_url"),
                jsonObject.optString("link_url"),
                jsonObject.optString("source")
        )

private fun String.jsonStringToProductItems(): List<ProductItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createProductItem(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createProductItem(jsonObject: JSONObject): ProductItem =
        ProductItem(
            jsonObject.optInt("id"),
            jsonObject.optString("name"),
            "Rp",
            jsonObject.optInt("price"),
            jsonObject.optString("discount"),
            jsonObject.optString("brand"),
            jsonObject.optString("link_url"),
            jsonObject.optString("image_url"),
            jsonObject.optDouble("rating").toFloat(),
            jsonObject.optInt("reviews")
        )

private fun String.jsonStringToCouponItems(): List<Coupon>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> createCouponItem(jsonObject) }
                .shuffled()
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun createCouponItem(jsonObject: JSONObject): Coupon =
        Coupon(
            jsonObject.optInt("id"),
            jsonObject.optString("title"),
            jsonObject.optString("brand"),
            jsonObject.optString("start_date"),
            jsonObject.optString("end_date"),
            jsonObject.optInt("remain"),
            jsonObject.optString("link_url"),
            jsonObject.optString("image_url")
        )

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