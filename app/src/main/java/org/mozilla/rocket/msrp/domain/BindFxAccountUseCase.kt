package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.SignInError
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.map

class BindFxAccountUseCase(private val userRepository: UserRepository) {

    suspend operator fun invoke(jwt: String?): Result<Unit, Error> {
        if (jwt == null) {
            return Result.error(error = Error.UnknownError("jwt is null"))
        }
        return userRepository.signInWithCustomToken(jwt).map(
            transformResult = {},
            transformError = {
                when (it) {
                    is SignInError.UnknownError -> Error.UnknownError(it.message)
                }
            }
        )
    }

    sealed class Error {
        data class UnknownError(val message: String) : Error()
    }
}