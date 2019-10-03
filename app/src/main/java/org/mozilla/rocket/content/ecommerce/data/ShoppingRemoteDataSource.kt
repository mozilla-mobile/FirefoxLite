package org.mozilla.rocket.content.ecommerce.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.util.safeApiCall
import java.net.URL

class ShoppingRemoteDataSource : ShoppingDataSource {

    override suspend fun getDeals(): Result<DealEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getDealsApiEndpoint())
                Success(DealEntity.fromJson(responseBody))
            },
            errorMessage = "Unable to get remote deals products"
        )
    }

    override suspend fun getCoupons(): Result<DealEntity> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getCouponsApiEndpoint())
                Success(DealEntity.fromJson(responseBody))
            },
            errorMessage = "Unable to get remote coupons"
        )
    }

    override suspend fun getVouchers(): Result<String> = withContext(Dispatchers.IO) {
        val vouchers = FirebaseHelper.getFirebase().getRcString(STR_E_COMMERCE_SHOPPING_LINKS)
        return@withContext if (vouchers.isNotEmpty()) {
            Success(vouchers)
        } else {
            Error(Exception("Unable to get remote vouchers"))
        }
    }

    override suspend fun getShoppingTabItems(): Result<String> = withContext(Dispatchers.IO) {
        val hasVoucherResult = getVouchers()
        return@withContext Success(
            if (hasVoucherResult is Success) {
                SHOPPING_TAB_ITEMS_WITH_VOUCHERS
            } else {
                SHOPPING_TAB_ITEMS
            }
        )
    }

    private fun getDealsApiEndpoint(): String {
        return "https://rocket-dev01.appspot.com/api/v1/content?locale=id-ID&category=shoppingDeal"
    }

    private fun getCouponsApiEndpoint(): String {
        return "https://rocket-dev01.appspot.com/api/v1/content?locale=id-ID&category=shoppingCoupon"
    }

    private fun getHttpResult(endpointUrl: String): String {
        var responseBody = HttpRequest.get(URL(endpointUrl), "")
        responseBody = responseBody.replace("\n", "")
        return responseBody
    }

    companion object {
        private const val STR_E_COMMERCE_SHOPPING_LINKS = "str_e_commerce_shoppinglinks"
        private const val SHOPPING_TAB_ITEMS = "[{\"type\":1},{\"type\":2}]"
        private const val SHOPPING_TAB_ITEMS_WITH_VOUCHERS = "[{\"type\":1},{\"type\":2},{\"type\":3}]"
    }
}