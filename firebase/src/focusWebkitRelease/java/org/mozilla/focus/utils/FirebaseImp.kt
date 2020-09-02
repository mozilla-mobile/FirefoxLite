/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.IOException
import java.util.HashMap

/**
 * It's a wrapper to communicate with Firebase
 */
open class FirebaseImp(fromResourceString: HashMap<String, Any>) : FirebaseContract(fromResourceString) {

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var firebaseAuth: FirebaseAuth

    override fun init(context: Context) {
        FirebaseApp.initializeApp(context)
        firebaseAuth = FirebaseAuth.getInstance()
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

    override fun getInstanceId(): String? = try {
        // This method is synchronized and runs in background thread
        FirebaseInstanceId.getInstance().id

        // below catch is important, if the app starts with Firebase disabled, calling getInstance()
        // will throw IllegalStateException
    } catch (e: IOException) {
        null
    }

    override fun getRegisterToekn(callback: (String?) -> Unit) {
        try {
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                val result = if (task.isSuccessful) {
                    task.result?.token
                } else {
                    null
                }
                callback(result)
            }
            // below catch is important, if the app starts with Firebase disabled, calling getInstance()
            // will throw IllegalStateException
        } catch (e: IOException) {
        }
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
                Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ${task.exception}")
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

    override fun setFirebaseUserProperty(context: Context, tag: String, value: String) {
        FirebaseAnalytics.getInstance(context).setUserProperty(tag, value)
    }

    override val uid: String?
        get() = firebaseAuth.uid

    override fun signInWithCustomToken(
        jwt: String,
        onSuccess: (String?, String?) -> Unit,
        onFail: (error: String) -> Unit
    ) {
        firebaseAuth.signInWithCustomToken(jwt).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "=====signInWithCustomToken success=====")
                fetchClaim(onSuccess)
            } else {
                onFail("signInWithCustomToken failed.:${task.exception}")
            }
        }
    }

    override fun initUserState(activity: Activity) {
        Log.d(TAG, "====initUserState====")

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "====signInAnonymously====")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e(TAG, "====signInAnonymously failure====")
                    }
                }
        }
    }

    override fun getUserToken(func: (String?) -> Unit) {
        // some http request(mission/redeem) needs user token to access the backend.
        // we need Firebase SDK to get the user token
        // TODO: maybe wrap this logic in a MSRP client SDK
        firebaseAuth.currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) { task.result?.token } else { null }

            func(token)
        } ?: func(null)
    }

    override fun isAnonymous(): Boolean? = firebaseAuth.currentUser?.isAnonymous

    private fun fetchClaim(onClaimFetched: (String?, String?) -> Unit) {

        firebaseAuth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->

            val fxUid = result.claims["fxuid"]?.toString()
            val oldFbUid = result.claims["oldFbUid"]?.toString()

            onClaimFetched(fxUid, oldFbUid)
        }
    }

    override fun refreshRemoteConfig(callback: (Boolean, e: Exception?) -> Unit) {
        remoteConfig.fetch(0).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Firebase RemoteConfig Fetch Successfully ")
                callback(true, null)
                remoteConfig.activateFetched()
            } else {
                Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ${task.exception}")
                callback(false, task.exception)
            }
        }
    }

    override fun enableCrashlytics(applicationContext: Context, enabled: Boolean) {
        if (enabled) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        } else {
            Log.d(TAG, "Disabling Crashlytics will need to restart the app")
            // see https://firebase.google.com/docs/crashlytics/customize-crash-reports?platform=android
        }
    }

    override fun enablePerformanceCollection(enabled: Boolean) {
    }

    override fun newTrace(key: String): FirebaseTrace? = null

    override fun retrieveTrace(key: String): FirebaseTrace? = null

    override fun cancelTrace(key: String): FirebaseTrace? = null

    override fun closeTrace(trace: FirebaseTrace): FirebaseTrace? = null
}
