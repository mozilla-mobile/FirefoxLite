package org.mozilla.rocket.content.ecommerce.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.util.AssetsUtils

class ShoppingLocalDataSource(private val appContext: Context) : ShoppingDataSource {

    override suspend fun getDeals(): Result<DealEntity> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            DealEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.product_mock_items))
        )
    }

    override suspend fun getCoupons(): Result<DealEntity> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            DealEntity.fromJson(AssetsUtils.loadStringFromRawResource(appContext, R.raw.coupon_mock_items))
        )
    }

    override suspend fun getVouchers(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            FirebaseHelper.getFirebase().getRcString(STR_E_COMMERCE_SHOPPING_LINKS)
        )
    }

    override suspend fun getShoppingTabItems(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext Result.Success(
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.shopping_tab_items) ?: ""
        )
    }

    companion object {
        private const val STR_E_COMMERCE_SHOPPING_LINKS = "str_e_commerce_shoppinglinks"
    }
}