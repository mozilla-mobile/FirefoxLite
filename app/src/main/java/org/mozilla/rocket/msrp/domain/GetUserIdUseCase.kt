package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.UserRepository

class GetUserIdUseCase(
    private val userRepository: UserRepository
) {

    operator fun invoke(): String = userRepository.getUserId()
}