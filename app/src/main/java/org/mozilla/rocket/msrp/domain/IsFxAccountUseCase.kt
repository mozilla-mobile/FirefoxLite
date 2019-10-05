package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.UserRepository

class IsFxAccountUseCase(
    private val userRepository: UserRepository
) {

    operator fun invoke(): Boolean = userRepository.isFxAccount()
}