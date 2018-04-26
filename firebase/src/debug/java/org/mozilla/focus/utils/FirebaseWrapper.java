/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.example.firebase.BuildConfig;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * It's a wrapper to communicate with Firebase
 */
abstract class FirebaseWrapper {




    private static final String TAG = "FirebaseWrapper";

    // Instance of FirebaseWrapper that provides default values
    static FirebaseWrapper instance;


    // ==== Crashlytics =====
    private static final String FIREBASE_CRASH_HANDLER_CLASS = "com.crashlytics.android.core.CrashlyticsUncaughtExceptionHandler";
    private static final String FIREBASE_CRASH_HANDLER_DEFAULT = "defaultHandler";

    private static Thread.UncaughtExceptionHandler systemCrashHandler;
    private static Thread.UncaughtExceptionHandler firebaseCrashHandler;


    // ==== Remote Config =====
    // FirebaseRemoteConfig has access to context internally so it need to be WeakReference
    private static WeakReference<FirebaseRemoteConfig> remoteConfig;

    // An app can fetch a maximum of 5 times in a 60 minute window before the SDK begins to throttle.
    // See: https://firebase.google.com/docs/remote-config/android#caching_and_throttling
    private static final long DEFAULT_CACHE_EXPIRATION_IN_SECONDS = 3600; // 1 hour in seconds.

    // Cache threshold for remote config
    private static long remoteConfigCacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_IN_SECONDS;


    // get Remote Config string
    static String getRcString(@NonNull Context context, @NonNull String key) {
        if (instance == null) {
            Log.e(TAG, "getRcString: failed, FirebaseWrapper not initialized");
            return "";
        }
        // if remoteConfig is not initialized, we go to default config directly
        if (remoteConfig == null) {
            final Object value = instance.getRemoteConfigDefault(context).get(key);
            if (value instanceof String) {
                return (String) value;
            } else {
                return "";
            }
        }

        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config != null) {
            return config.getString(key);
        }
        return "";
    }


    @WorkerThread
    static void updateInstanceId(boolean enable) {
        if (enable) {
            // This method is synchronized and runs in background thread
            FirebaseInstanceId.getInstance().getToken();
        } else {
            try {
                // This method is synchronized and runs in background thread
                FirebaseInstanceId.getInstance().deleteInstanceId();
            } catch (IOException e) {
                Log.e(TAG, "FCM failed to deleteInstanceId");
            }
        }

    }

    static void enableCloudMessaging(Context context, String componentName, boolean enable) {

        final ComponentName component = new ComponentName(context, componentName);

        final int newState = enable ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        context.getPackageManager().setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP);

    }

    static void initCrashlytics() {
        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();

        try {
            // If the cached handler if from Firebase
            if (handler != null && handler.getClass().getName().equals(FIREBASE_CRASH_HANDLER_CLASS)) {
                firebaseCrashHandler = handler;
                // We use reflection to get it's  systemHandler. Which should be RuntimeInit$UncaughtHandler
                final Field systemHandler = handler.getClass().getDeclaredField(FIREBASE_CRASH_HANDLER_DEFAULT); //NoSuchFieldException
                // Make it accessible
                systemHandler.setAccessible(true);

                systemCrashHandler = (Thread.UncaughtExceptionHandler) systemHandler.get(handler);

            } else {

                firebaseCrashHandler = null;
                systemCrashHandler = handler;
            }

        } catch (IllegalAccessException | NoSuchFieldException e) {

            firebaseCrashHandler = null;
            systemCrashHandler = handler;

            Log.e(TAG, "initCrashlytics failed: " + e);

        }

    }

    // Replace DefaultUncaughtExceptionHandler with our naive implementation
    // We don't need to cache the original UncaughtExceptionHandler
    // If we want to restart crashlytics, we just restart the app and don't call this method here.
    static void enableCrashlytics(boolean enable) {

        if (enable && firebaseCrashHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(firebaseCrashHandler);
        } else if (systemCrashHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(systemCrashHandler);
        }
    }

    static void enableAnalytics(Context context, boolean enable) {

        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable);
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    static void enableRemoteConfig(Context context, boolean enable) {
        if (!enable) {
            remoteConfig = null;
            return;
        }


        final FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        remoteConfig = new WeakReference<>(config);
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
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
        refreshRemoteConfig();
    }

    // Call this method to refresh the value in remote config.
    // Client code can access the remote config value in UI thread. But if the work here is not done,
    // it'll still see the old value.
    private static void refreshRemoteConfig() {
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
                } else {
                    Log.d(TAG, "Firebase RemoteConfig Fetch Failed: ");
                }
            }
        });
    }

    // Client code must implement this method so it's not static here.
    abstract HashMap<String, Object> getRemoteConfigDefault(Context context);
}
