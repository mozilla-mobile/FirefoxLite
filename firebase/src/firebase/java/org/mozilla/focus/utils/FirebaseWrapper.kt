/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.Size
import android.support.annotation.WorkerThread
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.HashMap

/**
 * It's a wrapper to communicate with Firebase
 */
abstract class FirebaseWrapper : FirebaseContract{

    interface RemoteConfigFetchCallback {
        fun onFetched()
        //        void onFailed();
    }

    // Client code must implement this method so it's not static here.
    internal abstract fun getRemoteConfigDefault(context: Context): HashMap<String, Any>

    // get Remote Config string
    override fun getRcString(context: Context, key: String): String {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized")
            throwRcNotInitException()

            return FIREBASE_STRING_DEFAULT
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = instance!!.getRemoteConfigDefault(context)[key]
            if (value is String) {
                return value
            } else {
                throwGetValueException("getRcString")
            }
        }

        val config = remoteConfig!!.get()
        if (config != null) {
            return config.getString(key)
        }
        throwGetValueException("getRcString")
        return FIREBASE_STRING_DEFAULT
    }

    override fun prettify(string: String): String {
        return string.replace(NEWLINE_PLACE_HOLDER, "\n")
    }

    override fun getRcLong(context: Context, key: String): Long {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized")
            throwRcNotInitException()
            return FIREBASE_LONG_DEFAULT
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = instance!!.getRemoteConfigDefault(context)[key]
            if (value is Int) {
                return value.toLong()
            } else if (value is Long) {
                return (value as Long?)!!
            }
            throwGetValueException("getRcLong")
            return FIREBASE_LONG_DEFAULT
        }

        val config = remoteConfig!!.get()
        if (config != null) {
            // config.getValue will never return null (checked from FirebaseRemoteConfig‘s decompiled source)
            return config.getValue(key).asLong()
        }
        throwGetValueException("getRcLong")
        return FIREBASE_LONG_DEFAULT
    }

    override fun getRcBoolean(context: Context, key: String): Boolean {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized")
            throwRcNotInitException()
            return FIREBASE_BOOLEAN_DEFAULT
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = instance!!.getRemoteConfigDefault(context)[key]
            if (value is Boolean) {
                return value
            }
            throwGetValueException("getRcBoolean")
            return FIREBASE_BOOLEAN_DEFAULT
        }

        val config = remoteConfig!!.get()
        if (config != null) {
            // config.getValue will never return null (checked from FirebaseRemoteConfig‘s decompiled source)
            return config.getValue(key).asBoolean()
        }
        throwGetValueException("getRcBoolean")
        return FIREBASE_BOOLEAN_DEFAULT
    }

    @WorkerThread
    override fun deleteInstanceId() {
        try {
            // This method is synchronized and runs in background thread
            FirebaseInstanceId.getInstance().deleteInstanceId()

            // below catch is important, if the app starts with Firebase disabled, calling getInstance()
            // will throw IllegalStateException
        } catch (e: IOException) {
            Log.e(TAG, "FirebaseInstanceId update failed ", e)
        }
    }


    override fun enableCloudMessaging(context: Context, componentName: String, enable: Boolean) {

        val component = ComponentName(context, componentName)

        val newState = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(
            component, newState, PackageManager.DONT_KILL_APP
        )
    }

    override fun enableAnalytics(context: Context, enable: Boolean) {

        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable)
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    override fun enableRemoteConfig(context: Context, callback: RemoteConfigFetchCallback) {

        val config = FirebaseRemoteConfig.getInstance()
        remoteConfig = WeakReference(config)
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(developerModeEnabled).build()
        config.setConfigSettings(configSettings)
        if (instance != null) {
            config.setDefaults(instance!!.getRemoteConfigDefault(context))
        }
        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (config.info.configSettings.isDeveloperModeEnabled) {
            remoteConfigCacheExpirationInSeconds = 0
        }

        config.fetch(remoteConfigCacheExpirationInSeconds).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Firebase RemoteConfig Fetch Successfully ")
                config.activateFetched()
                callback?.onFetched()
            } else {
                Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ")
            }
        }
    }

    override fun setDeveloperModeEnabled(developerModeEnabled: Boolean) {
        FirebaseWrapper.developerModeEnabled = developerModeEnabled
    }

    // If Firebase is not initialized, getInstance() will throw an exception here
    // Since  This method is for debugging, return empty string is acceptable
    override val fcmToken: String?
        get() {
            try {
                return FirebaseInstanceId.getInstance().token
            } catch (e: Exception) {
                Log.e(TAG, "getGcmToken: ", e)
                return FIREBASE_STRING_DEFAULT
            }
        }

    override fun event(context: Context?, @Size(min = 1L, max = 40L) key: String, param: Bundle?) {
        if (context == null) {
            return
        }
        FirebaseAnalytics.getInstance(context).logEvent(key, param)
    }

    private fun throwGetValueException(method: String) {
        if (developerModeEnabled) {
            throw RuntimeException("Calling FirebaseWrapper.$method failed")
        }
    }

    private fun throwRcNotInitException() {
        if (developerModeEnabled) {
            throw IllegalStateException("FirebaseWrapper not initialized")
        }
    }

    companion object {
        private val TAG = "FirebaseWrapper"

        // ==== Remote Config =====
        // An app can fetch a maximum of 5 times in a 60 minute window before the SDK begins to throttle.
        // See: https://firebase.google.com/docs/remote-config/android#caching_and_throttling
        private const val DEFAULT_CACHE_EXPIRATION_IN_SECONDS: Long = 3600 // 1 hour in seconds.
        private const val FIREBASE_BOOLEAN_DEFAULT = false
        private const val FIREBASE_LONG_DEFAULT = 0L
        private const val FIREBASE_STRING_DEFAULT = ""
        private const val NEWLINE_PLACE_HOLDER = "<BR>"

        // FirebaseRemoteConfig has access to context internally so it need to be WeakReference
        private var remoteConfig: WeakReference<FirebaseRemoteConfig>? = null

        // Cache threshold for remote config
        private var remoteConfigCacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_IN_SECONDS


        private var developerModeEnabled: Boolean = false


        // ==== Instance of FirebaseWrapper that provides default values ====
        var instance: FirebaseWrapper? = null

        fun initInternal(wrapper: FirebaseWrapper) {
            if (instance != null) {
                return
            }
            instance = wrapper
        }

    }
}
