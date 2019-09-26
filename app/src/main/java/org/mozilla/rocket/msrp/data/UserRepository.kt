package org.mozilla.rocket.msrp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.FirebaseHelper

open class UserRepository {

    suspend fun getUserToken(): String? = withContext(Dispatchers.IO) {
        FirebaseHelper.getUserToken()
    }
}
