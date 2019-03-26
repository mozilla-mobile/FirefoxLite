/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import android.os.Bundle

/**
 * It's a wrapper to communicate with Firebase
 */
class FirebaseNoOpImp(remoteConfigDefault: HashMap<String, Any> = HashMap()) : FirebaseContract(remoteConfigDefault) {

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

    override fun deleteInstanceId() {
    }

    override fun enableCloudMessaging(
        context: Context,
        componentName: String,
        enable: Boolean
    ) {
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
}
