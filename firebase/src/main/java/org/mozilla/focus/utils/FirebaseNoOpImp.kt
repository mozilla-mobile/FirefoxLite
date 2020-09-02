/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import java.util.HashMap

/**
 * It's a wrapper to communicate with Firebase
 */
open class FirebaseNoOpImp(remoteConfigDefault: HashMap<String, Any> = HashMap()) : FirebaseContract(remoteConfigDefault) {

    override fun init(context: Context) {
    }

    // get Remote Config string
    override fun getRcString(key: String): String {
        val value = remoteConfigDefault[key]
        if (value is String) {
            return value
        }
        return FIREBASE_STRING_DEFAULT
    }

    override fun getRcLong(key: String): Long {
        val value = remoteConfigDefault[key]
        if (value is Int) {
            return value.toLong()
        } else if (value is Long) {
            return value
        }
        return FIREBASE_LONG_DEFAULT
    }

    override fun getRcBoolean(key: String): Boolean {
        val value = remoteConfigDefault[key]
        if (value is Boolean) {
            return value
        }
        return FIREBASE_BOOLEAN_DEFAULT
    }

    override fun getInstanceId(): String? = null

    override fun getRegisterToekn(callback: (String?) -> Unit) {
    }

    override fun deleteInstanceId() {
    }

    override fun enableAnalytics(context: Context, enable: Boolean) {
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    override fun enableRemoteConfig(context: Context, callback: Callback) {
    }

    override fun setDeveloperModeEnabled(enable: Boolean) {
    }

    override fun getFcmToken() = ""

    override fun event(context: Context?, key: String, param: Bundle?) {
    }

    override fun setFirebaseUserProperty(context: Context, tag: String, value: String) {
    }

    override val uid: String?
        get() = null

    override fun signInWithCustomToken(
        jwt: String,
        onSuccess: (String?, String?) -> Unit,
        onFail: (error: String) -> Unit
    ) {
    }

    override fun initUserState(activity: Activity) {
    }

    override fun getUserToken(func: (String?) -> Unit) {
    }

    override fun isAnonymous(): Boolean? = null

    override fun refreshRemoteConfig(callback: (Boolean, e: Exception?) -> Unit) {
    }

    override fun enableCrashlytics(applicationContext: Context, enabled: Boolean) {
    }

    override fun enablePerformanceCollection(enabled: Boolean) {
    }

    override fun newTrace(key: String): FirebaseTrace? = null

    override fun retrieveTrace(key: String): FirebaseTrace? = null

    override fun cancelTrace(key: String): FirebaseTrace? = null

    override fun closeTrace(trace: FirebaseTrace): FirebaseTrace? = null
}
