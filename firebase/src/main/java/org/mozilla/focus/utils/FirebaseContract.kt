package org.mozilla.focus.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle

abstract class FirebaseContract(var remoteConfigDefault: HashMap<String, Any> = HashMap()) {

    abstract val uid: String?

    interface Callback {
        fun onRemoteConfigFetched()
        //        void onFailed();
    }

    abstract fun init(context: Context)

    abstract fun getFcmToken(): String?

    abstract fun getRcString(key: String): String

    abstract fun getRcLong(key: String): Long

    abstract fun getRcBoolean(key: String): Boolean

    abstract fun getInstanceId(): String?

    abstract fun getRegisterToekn(callback: (String?) -> Unit)

    abstract fun deleteInstanceId()

    abstract fun enableAnalytics(context: Context, enable: Boolean)

    abstract fun enableRemoteConfig(context: Context, callback: Callback)

    abstract fun setDeveloperModeEnabled(enable: Boolean)

    abstract fun event(context: Context?, key: String, param: Bundle?)

    abstract fun setFirebaseUserProperty(context: Context, tag: String, value: String)

    abstract fun signInWithCustomToken(
        jwt: String,
        onSuccess: (String?, String?) -> Unit,
        onFail: (error: String) -> Unit
    )
    abstract fun initUserState(activity: Activity)

    companion object {
        internal val TAG = "FirebaseContract"

        // ==== Remote Config =====
        // An app can fetch a maximum of 5 times in a 60 minute window before the SDK begins to throttle.
        // See: https://firebase.google.com/docs/remote-config/android#caching_and_throttling
        private const val DEFAULT_CACHE_EXPIRATION_IN_SECONDS: Long = 3600 // 1 hour
        const val FIREBASE_BOOLEAN_DEFAULT = false
        const val FIREBASE_LONG_DEFAULT = 0L
        const val FIREBASE_STRING_DEFAULT = ""

        // Cache threshold for remote config
        internal var remoteConfigCacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_IN_SECONDS

        var developerMode: Boolean = false
    }

    abstract fun getUserToken(func: (String?) -> Unit)

    abstract fun isAnonymous(): Boolean?

    abstract fun refreshRemoteConfig(callback: (Boolean, e: Exception?) -> Unit)

    abstract fun enableCrashlytics(applicationContext: Context, enabled: Boolean)

    abstract fun enablePerformanceCollection(enabled: Boolean)

    abstract fun newTrace(key: String): FirebaseTrace?

    abstract fun retrieveTrace(key: String): FirebaseTrace?

    abstract fun cancelTrace(key: String): FirebaseTrace?

    abstract fun closeTrace(trace: FirebaseTrace): FirebaseTrace?

    interface FirebaseTrace {
        fun getKey(): String
        fun start()
        fun stop()
    }
}