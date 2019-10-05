package org.mozilla.rocket.content.ecommerce.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepository

class GetCouponsUseCase(private val repository: ShoppingRepository) {

    suspend operator fun invoke(): Result<ApiEntity> {
        return repository.getCoupons()
    }
}