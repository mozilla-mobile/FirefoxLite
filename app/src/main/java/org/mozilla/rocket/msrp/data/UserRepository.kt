package org.mozilla.rocket.msrp.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.toFxResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class UserRepository {

    private val isFxAccount by lazy {
        MutableLiveData<Boolean>().apply { postValue(isFxAccount()) }
    }

    suspend fun getUserToken(): String? = withContext(Dispatchers.IO) {
        FirebaseHelper.getUserToken()
    }

    fun getUserId(): String = FirebaseHelper.getUid() ?: error("Not Login")

    fun isFxAccount(): Boolean = FirebaseHelper.isAnonymous() == false

    fun isFxAccountLiveData(): LiveData<Boolean> = isFxAccount

    suspend fun signInWithCustomToken(jwt: String): Result<Unit, SignInError> = withContext(Dispatchers.IO) {
        runCatching {
            suspendCoroutine<Unit> { continuation ->
                FirebaseHelper.signInWithCustomToken(
                    jwt,
                    onSuccess = { _, _ ->
                        Log.d(LOG_TAG, "onLoginSuccess success")
                        isFxAccount.postValue(true)
                        continuation.resume(Unit)
                    },
                    onFail = {
                        Log.d(LOG_TAG, "onLoginSuccess fail ===$it")
                        continuation.resumeWithException(Exception(it))
                    }
                )
            }
        }.toFxResult<Unit, SignInError> {
            SignInError.UnknownError(it.message ?: "")
        }
    }

    companion object {
        private const val LOG_TAG = "UserRepository"
    }
}

sealed class SignInError {
    data class UnknownError(val message: String) : SignInError()
}