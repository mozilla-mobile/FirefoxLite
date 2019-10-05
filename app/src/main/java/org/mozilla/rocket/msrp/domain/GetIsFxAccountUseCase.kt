package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.msrp.data.UserRepository

class GetIsFxAccountUseCase(
    private val userRepository: UserRepository
) {

    operator fun invoke(): LiveData<Boolean> = userRepository.isFxAccountLiveData()
}