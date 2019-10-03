package org.mozilla.rocket.content.ecommerce.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepository

class GetVouchersUseCase(private val repository: ShoppingRepository) {

    suspend operator fun invoke(): Result<String> {
        return repository.getVouchers()
    }
}