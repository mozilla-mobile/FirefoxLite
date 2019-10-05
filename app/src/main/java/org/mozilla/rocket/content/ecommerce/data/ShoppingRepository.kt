package org.mozilla.rocket.content.ecommerce.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

class ShoppingRepository(private val shoppingDataSource: ShoppingDataSource) {

    suspend fun getDeals(): Result<ApiEntity> {
        return shoppingDataSource.getDeals()
    }

    suspend fun getCoupons(): Result<ApiEntity> {
        return shoppingDataSource.getCoupons()
    }

    suspend fun getVouchers(): Result<String> {
        return shoppingDataSource.getVouchers()
    }

    suspend fun getShoppingTabItems(): Result<String> {
        return shoppingDataSource.getShoppingTabItems()
    }
}