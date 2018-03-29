/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.mozilla.focus.BuildConfig;

public class EspressoUtils {

    public static final String PREF_KEY_ENABLE_GEOLOCATION_PERMISSION_PROMPT = "espresso_geolocation_perm_prompt";

    public static boolean isAllowGeoPermissionPrompt(final Context context) {
        boolean ret = true;
        if (BuildConfig.DEBUG) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            ret = preferences.getBoolean(PREF_KEY_ENABLE_GEOLOCATION_PERMISSION_PROMPT, true);
        }
        return ret;
    }
}
