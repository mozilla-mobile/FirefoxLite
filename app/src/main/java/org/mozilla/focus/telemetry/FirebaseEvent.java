/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FirebaseHelper;

class FirebaseEvent {

    // limitation for event:
    // Event names can be up to 40 characters long, may only contain alphanumeric characters and
    // underscores ("_"), and must start with an alphabetic character. The "firebase_", "google_"
    // and "ga_" prefixes are reserved and should not be used.
    // see: https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event
    static final int MAX_LENGTH_EVENT_NAME = 40;
    static final String EVENT_NAME_SEPARATOR = "__";

    // limitation to param
    // You can associate up to 25 unique Params with each Event type. Param names can be up to 40
    // characters long, may only contain alphanumeric characters and underscores ("_"),
    // and must start with an alphabetic character. Param values can be up to 100 characters long.
    // The "firebase_", "google_" and "ga_" prefixes are reserved and should not be used.
    // see: https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Param
    static final int MAX_PARAM_SIZE = 25;
    static final int MAX_LENGTH_PARAM_NAME = 40;
    static final int MAX_LENGTH_PARAM_VALUE = 100;

    private String eventName;
    private Bundle eventParam;


    private FirebaseEvent(@NonNull String category, @NonNull String method, @Nullable String object, @Nullable String value) {

        // append null string will append 'null'
        // Android Studio also said we don't need to use a String builder here.
        this.eventName = method + EVENT_NAME_SEPARATOR + object + EVENT_NAME_SEPARATOR + value;
        if (!AppConstants.isReleaseBuild() && this.eventName.length() > MAX_LENGTH_EVENT_NAME) {
            // TODO: check eventName should start with[a-zA-Z] , contains only [a-zA-A0-9_], and shouldn't
            // TODO: start with ^(?!(firebase_|google_|ga_)).*
            throw new IllegalArgumentException(this.eventName + " exceeds Firebase event name length limit " + this.eventName.length() + " of " + MAX_LENGTH_EVENT_NAME);
        }
    }

    @CheckResult
    public static FirebaseEvent create(@NonNull String category, @NonNull String method, @Nullable String object, String value) {
        return new FirebaseEvent(category, method, object, value);

    }

    @CheckResult
    public static FirebaseEvent create(@NonNull String category, @NonNull String method, @Nullable String object) {
        return new FirebaseEvent(category, method, object, null);
    }

    public FirebaseEvent param(String name, String value) {
        if (this.eventParam == null) {
            this.eventParam = new Bundle();
        }
        // only throws in non-release build
        if (!AppConstants.isReleaseBuild() && this.eventParam.size() >= MAX_PARAM_SIZE) {
            throw new IllegalArgumentException("Exceeding limit of " + MAX_PARAM_SIZE + " param size");
        }

        this.eventParam.putString(safeParamLength(name, MAX_LENGTH_PARAM_NAME),
                safeParamLength(value, MAX_LENGTH_PARAM_VALUE));

        return this;
    }


    private static String safeParamLength(@NonNull final String str, final int end) {
        if (!AppConstants.isReleaseBuild() && (str.length() > end)) {
            throw new IllegalArgumentException("Exceeding limit of param content length:" + str.length() + " of " + end);
        }
        // TODO: check param name should start with[a-zA-Z] , contains only [a-zA-A0-9_], and shouldn't
        // TODO: start with ^(?!(firebase_|google_|ga_)).*
        return str.substring(0,
                Math.min(end, str.length()));
    }

    /*** Queue the events and let Firebase Analytics to decide when to upload to server
     *
     * @param context used for FirebaseAnalytics.getInstance() call.
     */
    public void event(Context context) {
        if (context == null) {
            return;
        }
        FirebaseHelper.event(context.getApplicationContext(), this.eventName, this.eventParam);
    }

    @VisibleForTesting
    public void setParam(Bundle bundle) {
        this.eventParam = bundle;
    }
}
