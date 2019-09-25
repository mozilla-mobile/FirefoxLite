package org.mozilla.rocket.msrp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.Result

open class UserRepository {

    suspend fun getUserToken(): Result<String, UserServiceError> = withContext(Dispatchers.IO) {
        val userToken = FirebaseHelper.getUserToken()
        if (userToken != null) {
            Result.success(userToken)
        } else {
            Result.error<String, UserServiceError>("get user token null", UserServiceError.GetUserTokenError)
        }
    }
}

sealed class UserServiceError {
    object GetUserTokenError : UserServiceError()
}
