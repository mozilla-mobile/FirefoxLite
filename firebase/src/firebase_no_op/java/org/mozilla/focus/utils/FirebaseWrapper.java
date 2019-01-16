/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.os.Bundle;

import java.util.HashMap;

/**
 * It's a wrapper to communicate with Firebase
 */
abstract class FirebaseWrapper {

    private static final String TAG = "FirebaseWrapper";
    private static final boolean FIREBASE_BOOLEAN_DEFAULT = false;
    private static final long FIREBASE_LONG_DEFAULT = 0L;
    private static final String FIREBASE_STRING_DEFAULT = "";

    // Instance of FirebaseWrapper that provides default values
    private static FirebaseWrapper instance;

    static FirebaseWrapper getInstance() {
        return instance;
    }

    static long getRcLong(Context context, String key) {
        if (instance == null) {
            return FIREBASE_LONG_DEFAULT;
        }
        final Object value = instance.getRemoteConfigDefault(context).get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return FIREBASE_LONG_DEFAULT;
    }

    // get Remote Config string
    static String getRcString(Context context, String key) {
        if (instance == null) {
            return FIREBASE_STRING_DEFAULT;
        }
        final Object value = instance.getRemoteConfigDefault(context).get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return FIREBASE_STRING_DEFAULT;
    }


    // get Remote Config boolean
    static boolean getRcBoolean(Context context, String key) {
        if (instance == null) {
            return FIREBASE_BOOLEAN_DEFAULT;
        }
        final Object value = instance.getRemoteConfigDefault(context).get(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        return FIREBASE_BOOLEAN_DEFAULT;
    }


    static void updateInstanceId(Context context, boolean enable) {
    }

    static void initInternal(FirebaseWrapper wrapper) {
        if (instance != null) {
            return;
        }
        instance = wrapper;
    }

    static void enableCloudMessaging(Context context, String componentName, boolean enable) {
    }


    // Replace DefaultUncaughtExceptionHandler with our naive implementation
    // We don't need to cache the original UncaughtExceptionHandler
    // If we want to restart crashlytics, we just restart the app and don't call this method here.
    static void enableCrashlytics(Context context, boolean enable) {

    }

    static void enableAnalytics(Context context, boolean enable) {
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    static void enableRemoteConfig(Context context, boolean enable, Runnable callback) {
    }

    static void setDeveloperModeEnabled(boolean developerModeEnabled) {
    }

    // Client code must implement this method so it's not static here.
    abstract HashMap<String, Object> getRemoteConfigDefault(Context context);

    abstract void refreshRemoteConfigDefault(Context context);

    public static String getFcmToken() {
        return FIREBASE_STRING_DEFAULT;
    }

    public static void event(Context context, String key, Bundle param) {
    }
}
