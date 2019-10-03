package org.mozilla.rocket.content.ecommerce.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.data.DealEntity
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepository

class GetCouponsUseCase(private val repository: ShoppingRepository) {

    suspend operator fun invoke(): Result<DealEntity> {
        return repository.getCoupons()
    }
}