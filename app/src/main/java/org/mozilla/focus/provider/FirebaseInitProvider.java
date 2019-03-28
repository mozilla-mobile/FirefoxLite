/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import android.util.Log;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FirebaseContract;
import org.mozilla.focus.utils.FirebaseHelper;

public class FirebaseInitProvider extends ContentProvider {
    private static final String TAG = "FirebaseInitProvider";

    public FirebaseInitProvider() {
    }

    public boolean onCreate() {
        final Context context = this.getContext();
        final boolean enable = TelemetryWrapper.isTelemetryEnabled(context);

        FirebaseContract contract;
        // Only debug build uses FirebaseNoOpImpl. Cause external contributors will not
        // work on Firebase related features.
        if (AppConstants.isBuiltWithFirebase()) {
            contract = FirebaseHelper.provideFirebaseImpl(context);
            Log.d(TAG, "We are using FirebaseImpl");
        } else {
            contract = FirebaseHelper.provideFirebaseNoOpImpl(context);
            Log.d(TAG, "We are using FirebaseNoOpImpl");
        }
        FirebaseHelper.init(context, enable, contract);
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}