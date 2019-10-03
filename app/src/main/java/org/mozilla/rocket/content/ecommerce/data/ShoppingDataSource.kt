package org.mozilla.rocket.content.ecommerce.data

import org.mozilla.rocket.content.Result

interface ShoppingDataSource {
    suspend fun getDeals(): Result<DealEntity>
    suspend fun getCoupons(): Result<DealEntity>
    suspend fun getVouchers(): Result<String>
    suspend fun getShoppingTabItems(): Result<String>
}