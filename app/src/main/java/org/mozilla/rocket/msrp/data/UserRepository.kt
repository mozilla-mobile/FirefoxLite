package org.mozilla.rocket.msrp.data

import org.mozilla.focus.utils.FirebaseHelper

open class UserRepository {

    suspend fun getUserToken(): String? {
        return FirebaseHelper.getUserToken()
    }
}
