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
import android.util.Log;

import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FirebaseHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class FirebaseEvent {

    // limitation for event:
    // Event names can be up to 40 characters long, may only contain alphanumeric characters and
    // underscores ("_"), and must start with an alphabetic character. The "firebase_", "google_"
    // and "ga_" prefixes are reserved and should not be used.
    // see: https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event
    @VisibleForTesting
    static final int MAX_LENGTH_EVENT_NAME = 40;
    // method+object shouldn't longer than 20 characters so value part can have enough space
    @VisibleForTesting
    static final int MAX_LENGTH_EVENT_NAME_PREFIX = 20;
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
    private static final String TAG = "FirebaseEvent";
    private static Set<String> existingPreferenceKey = new HashSet<>();

    private String eventName;
    private Bundle eventParam;


    FirebaseEvent(@NonNull String category, @NonNull String method, @Nullable String object, @Nullable String value) {

        // append null string will append 'null'
        // Android Studio also said we don't need to use a String builder here.
        final String eventNamePrefix = method + EVENT_NAME_SEPARATOR + object + EVENT_NAME_SEPARATOR;
        this.eventName = eventNamePrefix + value;

        final int prefixLength = eventNamePrefix.length();

        if (this.eventName.length() > MAX_LENGTH_EVENT_NAME) {

            if (prefixLength > MAX_LENGTH_EVENT_NAME_PREFIX) {
                handleError("Event[" + this.eventName + "]'s prefixLength too long  " + prefixLength + " of " + MAX_LENGTH_EVENT_NAME_PREFIX);
            }

            // TODO: check eventName should start with[a-zA-Z] , contains only [a-zA-A0-9_], and shouldn't
            // TODO: start with ^(?!(firebase_|google_|ga_)).*
            if (value != null && isValueInWhiteList(value)) {
                int acceptValueLength = MAX_LENGTH_EVENT_NAME - eventNamePrefix.length();
                int valueLength = value.length();
                this.eventName = eventNamePrefix + value.substring(valueLength - acceptValueLength, valueLength);
            } else {
                // No matter if value is null nor not, if the length of event name is too long, handle the error hera.
                handleError("Event[" + this.eventName + "] exceeds Firebase event name limit " + this.eventName.length() + " of " + MAX_LENGTH_EVENT_NAME);
            }
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
        if (this.eventParam.size() >= MAX_PARAM_SIZE) {
            handleError("Firebase event[" + eventName + "] has too many parameters");
        }

        this.eventParam.putString(safeParamLength(name, MAX_LENGTH_PARAM_NAME),
                safeParamLength(value, MAX_LENGTH_PARAM_VALUE));

        return this;
    }

    private static String safeParamLength(@NonNull final String str, final int end) {
        if (str.length() > end) {
            handleError("Exceeding limit of param content length:" + str.length() + " of " + end);
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
        if (TelemetryWrapper.isTelemetryEnabled(context)) {
            FirebaseHelper.event(context.getApplicationContext(), this.eventName, this.eventParam);
        }
    }

    @VisibleForTesting
    public void setParam(Bundle bundle) {
        this.eventParam = bundle;
    }


    private static void handleError(String msg) {
        if (AppConstants.isReleaseBuild()) {
            Log.e(TAG, msg);
        } else {
            throw new IllegalArgumentException(msg);
        }
    }

    private static boolean isValueInWhiteList(@NonNull String value) {
        return existingPreferenceKey.contains(value);
    }

    static void clearValueWhitelist() {
        existingPreferenceKey.clear();
    }

    static void addValueWhitelist(String preferenceKey) {
        existingPreferenceKey.add(preferenceKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FirebaseEvent)) {
            return false;
        }
        FirebaseEvent event = ((FirebaseEvent) obj);
        return this.eventName.equals(event.eventName) &&
                equalBundles(this.eventParam, event.eventParam);
    }

    private boolean equalBundles(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }

        if (!a.keySet().containsAll(b.keySet())) {
            return false;
        }

        for (String key : a.keySet()) {
            String valueOne = ((String) a.get(key));
            String valueTwo = ((String) b.get(key));
            if (!(valueOne != null && valueOne.equals(valueTwo))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {

        return Objects.hash(eventName, eventParam);
    }
}