package org.mozilla.rocket.content.ecommerce.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductItem
import org.mozilla.rocket.content.ecommerce.ui.adapter.Runway
import org.mozilla.rocket.content.ecommerce.ui.adapter.RunwayItem
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray
import java.util.*
import kotlin.random.Random

class ShoppingRepo(private val appContext: Context) {

    suspend fun getDeals(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            listOf(
                Runway(listOf(
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem()
                )),
                ProductCategory(UUID.randomUUID().toString(),
                    "Flash Deals",
                    getMockProductItems()?.subList(0, 10) ?: emptyList()
                ),
                ProductCategory(UUID.randomUUID().toString(),
                    "Editors' Choice",
                    getMockProductItems()?.subList(11, 20) ?: emptyList()
                ),
                ProductCategory(UUID.randomUUID().toString(),
                    "New Products Arrived",
                    getMockProductItems()?.subList(21, 30) ?: emptyList()
                )
            )
        }
    }

    suspend fun getCoupons(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<DelegateAdapter.UiModel>()
            list.add(Runway(listOf(
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem(),
                    generateFakeRunwayItem()
            )))
            list.addAll(AppConfigWrapper.getEcommerceCoupons())
            return@withContext list
        }
    }

    suspend fun getVouchers(): List<DelegateAdapter.UiModel> {
        return withContext(Dispatchers.IO) {
            AppConfigWrapper.getEcommerceVouchers()
        }
    }

    // TODO: remove test function
    private fun getPlaceholderImageUrl(w: Int, h: Int): String =
            "https://placeimg.com/$w/$h/animals?whatever=${Random.nextInt(0, 10)}"

    // TODO: remove test function
    private fun generateFakeRunwayItem(): RunwayItem =
            getPlaceholderImageUrl(400, 200).run { RunwayItem(this, this, this) }

    private fun getMockProductItems(): List<ProductItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.product_mock_items)
                ?.jsonStringToProductItems()
}

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
            jsonObject.getInt("id"),
            jsonObject.getString("name"),
            "Rp",
            jsonObject.getInt("price"),
            jsonObject.getString("discount"),
            jsonObject.getString("brand"),
            jsonObject.getString("link_url"),
            jsonObject.getString("image_url"),
            jsonObject.getDouble("rating").toFloat(),
            jsonObject.getInt("reviews")
        )