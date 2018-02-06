/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.lang.ref.WeakReference;

/**
 * Provide helper for Firebase functionality
 */
public class FirebaseHelper {

    public static String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    public static String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";

    private static WeakReference<FirebaseRemoteConfig> REMOTE_CONFIG;

    private static long CACHE_EXPIRATION = 3600; // 1 hour in seconds.
    private static String TAG = "FirebaseHelper";


    public static String getString(String s) {
        return REMOTE_CONFIG.get().getString(s);
    }

    public static Boolean getBoolean(String s) {
        return REMOTE_CONFIG.get().getBoolean(s);
    }

    public static Long getLong(String s) {
        return REMOTE_CONFIG.get().getLong(s);
    }

    public static void init() {

        REMOTE_CONFIG = new WeakReference<>(FirebaseRemoteConfig.getInstance());
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        REMOTE_CONFIG.get().setConfigSettings(configSettings);
        REMOTE_CONFIG.get().setDefaults(R.xml.remote_config_default);

        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (REMOTE_CONFIG.get().getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            CACHE_EXPIRATION = 0;
        }
        refresh();
    }

    // Call this method to refresh the value in remote config
    public static void refresh() {
        REMOTE_CONFIG.get().fetch(CACHE_EXPIRATION).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Successfully ");
                    REMOTE_CONFIG.get().activateFetched();
                } else {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Failed: ");
                }

            }
        });
    }
}
