package org.mozilla.rocket.content.ecommerce.data

import org.mozilla.rocket.content.Result

class ShoppingRepository(private val shoppingDataSource: ShoppingDataSource) {

    suspend fun getDeals(): Result<DealEntity> {
        return shoppingDataSource.getDeals()
    }

    suspend fun getCoupons(): Result<DealEntity> {
        return shoppingDataSource.getCoupons()
    }

    suspend fun getVouchers(): Result<String> {
        return shoppingDataSource.getVouchers()
    }

    suspend fun getShoppingTabItems(): Result<String> {
        return shoppingDataSource.getShoppingTabItems()
    }
}