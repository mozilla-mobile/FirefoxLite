/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;

import java.util.HashMap;

/**
 * It's a wrapper to communicate with Firebase
 */
abstract public class FirebaseWrapper {

    static String getRcString(String key) {
        return null;
    }

    public static void setDeveloperModeEnabled(boolean enabled) {
    }

    static void internalInit(final Context context, FirebaseWrapper wrapper) {
        // dummy with no implmenetation
    }

    // Client code must implement this method so it's not static here.
    abstract HashMap<String, Object> getRemoteConfigDefault(Context context);

}
