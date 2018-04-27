/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONObject;
import org.mozilla.focus.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;

// This class is to inject different FirebaseHelper implementation.
// I don't want to put it Inject.java because I only need to inject getRemoteConfigDefault() method
@WorkerThread
final class FirebaseHelperInject {

    private static final String REMOTE_CONFIG_JSON = "remote_config.json";
    private static final String TAG = "FirebaseHelperInject";

    // XML default value can't read l10n values, so we use code for default values.

    static HashMap<String, Object> getRemoteConfigDefault(Context context) {
        // This should only happen in the background thread during initialization in BlockingEnabler's doInBackground
        final boolean isWorkerThread = Looper.myLooper() != Looper.getMainLooper();
        // If we don't have read external storage permission, just don't bother reading the config file.
        if (isWorkerThread && canReadExternalStorage(context)) {
            try {
                return fromJsonOnDisk();
            } catch (Exception e) {
                Log.e(TAG, "Some problem when reading RemoteConfig file from storageDefault: " + e);
                // For any exception, we read the default resource file.
                return fromResourceString(context);
            }
        }

        return fromResourceString(context);
    }

    // Assume we already have read external storage permission
    // For any Exception, we'll handle them in the same way. So a general Exception should be fine.
    @WorkerThread
    private static HashMap<String, Object> fromJsonOnDisk() throws Exception {

        final File sdcard = Environment.getExternalStorageDirectory();

        // Check External Storage
        if (sdcard == null) {
            throw new Exception("No External Storage Available");
        }

        // Check if config file exist
        final File file = new File(sdcard, REMOTE_CONFIG_JSON);

        if (!file.exists()) {
            throw new FileNotFoundException("Can't find " + REMOTE_CONFIG_JSON);
        }

        // Read text from config file
        final StringBuilder text = new StringBuilder();

        // try with resource so br will call close() automatically
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        }

        // Parse JSON and put it into a HashMap
        final JSONObject jsonObject = new JSONObject(text.toString());
        final HashMap<String, Object> map = new HashMap<>();

        // Iterate the JSON and put the key-value pair in the HashMap
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            map.put(key, jsonObject.get(key));
        }


        return map;
    }

    // We need to provide different value for different build type for debug/testing purpose
    private static HashMap<String, Object> fromResourceString(Context context) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title));
        map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
        return map;
    }

    // Check if we have the permission. Currently it's only needed here so I hesitate to move it to a util class.
    private static boolean canReadExternalStorage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }
}
