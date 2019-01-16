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

    // Instance of FirebaseWrapper that provides default values
    private static FirebaseWrapper instance;


    // ==== Crashlytics =====
    private static final String DIR_FABRIC_ROOT = "//data//data//%s//files//.Fabric";
    private static final String DIR_FABRIC_CRASHLYTICS_CORE = DIR_FABRIC_ROOT + "//com.crashlytics.sdk.android.crashlytics-core";
    private static final String DIR_FABRIC_CRASHLYTICS_CORE_LOG = DIR_FABRIC_CRASHLYTICS_CORE + "//log-files";
    private static final String DIR_FABRIC_ANSWERS = DIR_FABRIC_ROOT + "//com.crashlytics.sdk.android:answers";
    private static final String DIR_FABRIC_SETTINGS = DIR_FABRIC_ROOT + "//io.fabric.sdk.android:fabric";


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
    private static boolean developerModeEnabled;


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

    private static void initCrashlytics(Context context) {
        try {
            final Fabric fabric = new Fabric.Builder(context)
                    .kits(new Crashlytics())
                    .debuggable(developerModeEnabled)           // Enables Crashlytics debugger
                    .build();
            Fabric.with(fabric);
            Log.i(TAG, "CrashlyticsInitProvider initialization successful");
        } catch (IllegalStateException e) {
            Log.e(TAG, "CrashlyticsInitProvider initialization unsuccessful", e);
        }
    }

    // If we disable Crashlytics at runtime, we'll need to switch to the origin UncaughtExceptionHandler
    private static void setupUncaughtExceptionHandler() {

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

            systemCrashHandler = handler;

            Log.e(TAG, "initCrashlytics failed: " + e);

        }
    }

    // Replace DefaultUncaughtExceptionHandler with our naive implementation
    // We don't need to cache the original UncaughtExceptionHandler
    // If we want to restart crashlytics, we just restart the app and don't call this method here.
    static void enableCrashlytics(Context context, boolean enable) {

        setupUncaughtExceptionHandler();

        if (enable) {
            initCrashlytics(context);
            if (firebaseCrashHandler != null) {
                Thread.setDefaultUncaughtExceptionHandler(firebaseCrashHandler);
            }
        } else if (systemCrashHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(systemCrashHandler);
            removeCrashReportDirectory(context);
        }
    }

    private static void removeCrashReportDirectory(Context context) {

        deleteAllFiles(context, DIR_FABRIC_CRASHLYTICS_CORE);
        deleteAllFiles(context, DIR_FABRIC_CRASHLYTICS_CORE_LOG);
        deleteAllFiles(context, DIR_FABRIC_ANSWERS);
        deleteAllFiles(context, DIR_FABRIC_SETTINGS);

    }

    private static void deleteAllFiles(Context context, String dirFabricCrashlyticsCore) {
        final String path = String.format(dirFabricCrashlyticsCore, context.getPackageName());
        final File crashlyticsReportDir = new File(path);
        try {
            if (crashlyticsReportDir.exists() && crashlyticsReportDir.isDirectory()) {
                for (File report : crashlyticsReportDir.listFiles()) {
                    report.delete();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Fail when deleting all files in : " + dirFabricCrashlyticsCore, e);
        }
    }

    static void enableAnalytics(Context context, boolean enable) {
        if (enable) {
            FirebaseApp.initializeApp(context);
        }
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable);
    }

    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    static void enableRemoteConfig(Context context, boolean enable, @Nullable Runnable callback) {
        if (!enable) {
            remoteConfig = null;
            return;
        }


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
    private static void refreshRemoteConfig(@Nullable final Runnable callback) {
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
                        callback.run();
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

    // Client code must implement this method so it's not static here.
    abstract void refreshRemoteConfigDefault(Context context);

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
