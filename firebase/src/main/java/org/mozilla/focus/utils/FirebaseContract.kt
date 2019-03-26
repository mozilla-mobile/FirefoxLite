package org.mozilla.focus.utils

import android.content.Context
import android.os.Bundle

abstract class FirebaseContract(var remoteConfigDefault: HashMap<String, Any> = HashMap()) {

    interface Callback {
        fun onRemoteConfigFetched()
        //        void onFailed();
    }

    abstract fun init(context: Context)

    abstract fun getFcmToken(): String?

    abstract fun getRcString(key: String): String

    abstract fun getRcLong(key: String): Long

    abstract fun getRcBoolean(key: String): Boolean

    abstract fun deleteInstanceId()

    abstract fun enableCloudMessaging(context: Context, componentName: String, enable: Boolean)

    abstract fun enableAnalytics(context: Context, enable: Boolean)

    abstract fun enableRemoteConfig(context: Context, callback: Callback)

    abstract fun setDeveloperModeEnabled(enable: Boolean)

    abstract fun event(context: Context?, key: String, param: Bundle?)

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
}