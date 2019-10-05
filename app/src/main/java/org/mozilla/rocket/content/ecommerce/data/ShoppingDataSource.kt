package org.mozilla.rocket.content.ecommerce.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity

interface ShoppingDataSource {
    suspend fun getDeals(): Result<ApiEntity>
    suspend fun getCoupons(): Result<ApiEntity>
    suspend fun getVouchers(): Result<String>
    suspend fun getShoppingTabItems(): Result<String>
}