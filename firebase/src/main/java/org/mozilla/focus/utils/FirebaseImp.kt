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
import java.util.HashMap

/**
 * It's a wrapper to communicate with Firebase
 */
class FirebaseImpl(fromResourceString: HashMap<String, Any>) : FirebaseContract(fromResourceString) {

    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun init(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    // get Remote Config string
    override fun getRcString(key: String): String {
        return remoteConfig.getString(key)
    }

    override fun getRcLong(key: String): Long {
        return remoteConfig.getLong(key)
    }

    override fun getRcBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
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

    override fun enableAnalytics(context: Context, enable: Boolean) {

        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable)
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    override fun enableRemoteConfig(context: Context, callback: Callback) {

        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(developerMode).build()
        remoteConfig.setConfigSettings(configSettings)
        if (remoteConfigDefault.size > 0) {
            remoteConfig.setDefaults(remoteConfigDefault)
        }
        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (remoteConfig.info.configSettings.isDeveloperModeEnabled) {
            remoteConfigCacheExpirationInSeconds = 0
        }

        remoteConfig.fetch(remoteConfigCacheExpirationInSeconds).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Firebase RemoteConfig Fetch Successfully ")
                remoteConfig.activateFetched()
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
}
