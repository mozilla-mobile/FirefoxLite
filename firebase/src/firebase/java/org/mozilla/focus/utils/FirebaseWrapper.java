/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

import io.fabric.sdk.android.Fabric;

/**
 * It's a wrapper to communicate with Firebase
 */
abstract class FirebaseWrapper {

    private static final String TAG = "FirebaseWrapper";
    private static final boolean FIREBASE_BOOLEAN_DEFAULT = false;
    private static final long FIREBASE_LONG_DEFAULT = 0L;
    private static final String FIREBASE_STRING_DEFAULT = "";
    private static final String NEWLINE_PLACE_HOLDER = "<BR>";

    // Instance of FirebaseWrapper that provides default values
    private static FirebaseWrapper instance;



    // ==== Remote Config =====
    // FirebaseRemoteConfig has access to context internally so it need to be WeakReference
    private static WeakReference<FirebaseRemoteConfig> remoteConfig;

    // An app can fetch a maximum of 5 times in a 60 minute window before the SDK begins to throttle.
    // See: https://firebase.google.com/docs/remote-config/android#caching_and_throttling
    private static final long DEFAULT_CACHE_EXPIRATION_IN_SECONDS = 3600; // 1 hour in seconds.

    // Cache threshold for remote config
    private static long remoteConfigCacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_IN_SECONDS;
    private static boolean developerModeEnabled;

    public interface RemoteConfigFetchCallback {
        void onFetched();
//        void onFailed();
    }

    // get Remote Config string
    static String getRcString(@NonNull Context context, @NonNull String key) {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized");
            throwRcNotInitException();

            return FIREBASE_STRING_DEFAULT;
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            final Object value = instance.getRemoteConfigDefault(context).get(key);
            if (value instanceof String) {
                return (String) value;
            } else {
                throwGetValueException("getRcString");
            }
        }

        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config != null) {
            return config.getString(key);
        }
        throwGetValueException("getRcString");
        return FIREBASE_STRING_DEFAULT;
    }

    static String prettify(@NonNull String string) {
        return string.replace(NEWLINE_PLACE_HOLDER, "\n");
    }

    static long getRcLong(Context context, String key) {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized");
            throwRcNotInitException();
            return FIREBASE_LONG_DEFAULT;

        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            final Object value = instance.getRemoteConfigDefault(context).get(key);
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Long) {
                return (Long) value;
            }
            throwGetValueException("getRcLong");
            return FIREBASE_LONG_DEFAULT;
        }

        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config != null) {
            // config.getValue will never return null (checked from FirebaseRemoteConfig‘s decompiled source)
            return config.getValue(key).asLong();
        }
        throwGetValueException("getRcLong");
        return FIREBASE_LONG_DEFAULT;


    }


    static boolean getRcBoolean(Context context, String key) {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized");
            throwRcNotInitException();
            return FIREBASE_BOOLEAN_DEFAULT;
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            final Object value = instance.getRemoteConfigDefault(context).get(key);
            if (value instanceof Boolean) {
                return (boolean) value;
            }
            throwGetValueException("getRcBoolean");
            return FIREBASE_BOOLEAN_DEFAULT;
        }

        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config != null) {
            // config.getValue will never return null (checked from FirebaseRemoteConfig‘s decompiled source)
            return config.getValue(key).asBoolean();
        }
        throwGetValueException("getRcBoolean");
        return FIREBASE_BOOLEAN_DEFAULT;


    }

    @WorkerThread
    static void updateInstanceId(Context context, boolean enable) {
        try {
            if (enable) {
                // if FirebaseApp has already initialized, it'll return it's instance.
                // if the app starts with Firebase disabled, call initializeApp() before getInstance()
                FirebaseApp.initializeApp(context);
                // This method is synchronized and runs in background thread
                FirebaseInstanceId.getInstance().getToken();
            } else {
                // This method is synchronized and runs in background thread
                FirebaseInstanceId.getInstance().deleteInstanceId();

            }
            // below catch is important, if the app starts with Firebase disabled, calling getInstance()
            // will throw IllegalStateException
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, "FirebaseInstanceId update failed ", e);
        }

    }

    static void initInternal(FirebaseWrapper wrapper) {
        if (instance != null) {
            return;
        }
        instance = wrapper;
    }

    static FirebaseWrapper getInstance() {
        return instance;
    }


    static void enableCloudMessaging(Context context, String componentName, boolean enable) {

        final ComponentName component = new ComponentName(context, componentName);

        final int newState = enable ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        context.getPackageManager().setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP);
    }

    static void enableAnalytics(Context context, boolean enable) {

        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable);
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    static void enableRemoteConfig(Context context, RemoteConfigFetchCallback callback) {

        final FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        remoteConfig = new WeakReference<>(config);
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(developerModeEnabled)
                .build();
        config.setConfigSettings(configSettings);
        if (instance != null) {
            config.setDefaults(instance.getRemoteConfigDefault(context));
        }
        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (config.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            remoteConfigCacheExpirationInSeconds = 0;
        }
        refreshRemoteConfig(callback);
    }

    // Call this method to refresh the value in remote config.
    // Client code can access the remote config value in UI thread. But if the work here is not done,
    // it'll still see the old value.
    private static void refreshRemoteConfig(final RemoteConfigFetchCallback callback) {
        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config == null) {
            return;
        }
        config.fetch(remoteConfigCacheExpirationInSeconds).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase RemoteConfig Fetch Successfully ");
                    config.activateFetched();
                    if (callback != null) {
                        callback.onFetched();
                    }
                } else {
                    Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ");
                }
            }
        });
    }

    static void setDeveloperModeEnabled(boolean developerModeEnabled) {
        FirebaseWrapper.developerModeEnabled = developerModeEnabled;
    }

    // Client code must implement this method so it's not static here.
    abstract HashMap<String, Object> getRemoteConfigDefault(Context context);

    @Nullable
    public static String getFcmToken() {
        try {
            return FirebaseInstanceId.getInstance().getToken();
        } catch (Exception e) {
            // If Firebase is not initialized, getInstance() will throw an exception here
            // Since  This method is for debugging, return empty string is acceptable
            Log.e(TAG, "getGcmToken: ", e);
            return FIREBASE_STRING_DEFAULT;
        }
    }

    private static void throwGetValueException(String method) {
        if (developerModeEnabled) {
            throw new RuntimeException("Calling FirebaseWrapper." + method + " failed");
        }
    }

    private static void throwRcNotInitException() {
        if (developerModeEnabled) {
            throw new IllegalStateException("FirebaseWrapper not initialized");
        }
    }

    public static void event(Context context, String key, Bundle param) {
        if (context == null || key == null) {
            return;
        }
        FirebaseAnalytics.getInstance(context).logEvent(key, param);
    }
}
