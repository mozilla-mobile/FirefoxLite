/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * It's a wrapper to communicate with Firebase
 */
abstract class FirebaseWrapper {

    private static final long DEFAULT_CACHE_EXPIRATION_IN_SECONDS = 3600; // 1 hour in seconds.

    private static final String TAG = "FirebaseHelper";

    // FirebaseRemoteConfig has access to context internally so it need to be WeakReference
    private static WeakReference<FirebaseRemoteConfig> remoteConfig;

    // We allow the client code to modify this with setter
    private static boolean isDebugMode;

    private static long cacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_IN_SECONDS;


    // get Remote Config string
    static String getRcString(String key) {
        final FirebaseRemoteConfig config = remoteConfig.get();
        if (config != null) {
            return config.getString(key);
        }
        return null;
    }

    static void setDeveloperModeEnabled(boolean enabled) {
        isDebugMode = enabled;
    }


    // This need to be run in worker thread since FirebaseRemoteConfigSettings has IO access
    @WorkerThread
    static void internalInit(final Context context, FirebaseWrapper wrapper) {
        if (wrapper == null) {
            Log.e(TAG, "FirebaseWrapper Not initialized");
            return;
        }
        // Init remote config
        final FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        remoteConfig = new WeakReference<>(config);
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(isDebugMode)
                .build();
        config.setConfigSettings(configSettings);
        config.setDefaults(wrapper.getRemoteConfigDefault(context));

        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (config.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpirationInSeconds = 0;
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
        config.fetch(cacheExpirationInSeconds).addOnCompleteListener(new OnCompleteListener<Void>() {
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
