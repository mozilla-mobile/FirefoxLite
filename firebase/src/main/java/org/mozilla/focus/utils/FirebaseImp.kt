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
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * It's a wrapper to communicate with Firebase
 */
class FirebaseImpl : FirebaseContract() {

    private var remoteConfig: WeakReference<FirebaseRemoteConfig>? = null

    override fun init(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    // get Remote Config string
    override fun getRcString(context: Context, key: String): String {

        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = remoteConfigDefault[key]
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

    override fun getRcLong(context: Context, key: String): Long {
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = remoteConfigDefault[key]
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
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            val value = remoteConfigDefault[key]
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

    override fun enableCloudMessaging(
        context: Context,
        componentName: String,
        enable: Boolean
    ) {

        val component = ComponentName(context, componentName)

        val newState = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        context.packageManager.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
    }

    override fun enableAnalytics(context: Context, enable: Boolean) {

        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable)
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    override fun enableRemoteConfig(context: Context, callback: Callback) {

        val config = FirebaseRemoteConfig.getInstance()
        remoteConfig = WeakReference(config)
        val configSettings = FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(developerMode).build()
        config.setConfigSettings(configSettings)
        if (remoteConfigDefault.size > 0) {
            config.setDefaults(remoteConfigDefault)
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
                callback.onRemoteConfigFetched()
            } else {
                Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ")
            }
        }
    }

    override fun setDeveloperModeEnabled(enable: Boolean) {
        developerMode = enable
    }

    override fun getFcmToken(): String? {
        return try {
            FirebaseInstanceId.getInstance().token
        } catch (e: Exception) {
            Log.e(TAG, "getGcmToken: ", e)
            ""
        }
    }

    override fun event(
        context: Context?,
        @Size(min = 1L, max = 40L) key: String,
        param: Bundle?
    ) {
        if (context == null) {
            return
        }
        FirebaseAnalytics.getInstance(context).logEvent(key, param)
    }

    private fun throwGetValueException(method: String) {
        if (developerMode) {
            throw RuntimeException("Calling FirebaseImpl.$method failed")
        }
    }

}
