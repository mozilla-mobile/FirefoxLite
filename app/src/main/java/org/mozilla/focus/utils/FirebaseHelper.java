/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Provide helper for Firebase functionality
 */
public class FirebaseHelper {

    public static final String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    public static final String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";
    private static final String TAG = "FirebaseHelper";

    private static WeakReference<FirebaseRemoteConfig> REMOTE_CONFIG;
    private static long CACHE_EXPIRATION = 3600; // 1 hour in seconds.

    public static String getString(String key) {
        final FirebaseRemoteConfig config = REMOTE_CONFIG.get();
        if (config != null) {
            return config.getString(key);
        }
        return null;
    }

    public static void init(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                internalInit(context);
            }
        }).start();
    }

    private static void internalInit(final Context context) {

        final FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        REMOTE_CONFIG = new WeakReference<>(config);
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        config.setConfigSettings(configSettings);
        config.setDefaults(getRemoteConfigDefault(context));

        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (config.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            CACHE_EXPIRATION = 0;
        }
        refresh();
    }

    // Call this method to refresh the value in remote config
    public static void refresh() {
        final FirebaseRemoteConfig config = REMOTE_CONFIG.get();
        if (config == null) {
            return;
        }
        config.fetch(CACHE_EXPIRATION).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Successfully ");
                    config.activateFetched();
                } else {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Failed: ");
                }

            }
        });
    }

    public static HashMap<String, Object> getRemoteConfigDefault(Context context) {

        final HashMap<String, Object> map = new HashMap<>();
        map.put(RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title));
        map.put(RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
        return map;
    }
}
