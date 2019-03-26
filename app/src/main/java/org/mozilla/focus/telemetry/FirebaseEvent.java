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

import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.rocket.util.LoggerWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private static HashMap<String, String> prefKeyWhitelist = new HashMap<>();

    private String eventName;
    private Bundle eventParam;


    FirebaseEvent(@NonNull String category, @NonNull String method, @Nullable String object, @Nullable String value) {

        // append null string will append 'null'
        // Android Studio also said we don't need to use a String builder here.
        final String eventNamePrefix = method + EVENT_NAME_SEPARATOR + object + EVENT_NAME_SEPARATOR;
        this.eventName = eventNamePrefix + value;

        final int prefixLength = eventNamePrefix.length();

        // TODO: check eventName should start with[a-zA-Z] , contains only [a-zA-A0-9_], and shouldn't
        // TODO: start with ^(?!(firebase_|google_|ga_)).*
        // validate the length
        if (this.eventName.length() > MAX_LENGTH_EVENT_NAME) {

            // we only care about prefixLength if the total length is too long
            if (prefixLength > MAX_LENGTH_EVENT_NAME_PREFIX) {
                LoggerWrapper.throwOrWarn(TAG, "Event[" + this.eventName + "]'s prefixLength too long  " + prefixLength + " of " + MAX_LENGTH_EVENT_NAME_PREFIX);
            }

            LoggerWrapper.throwOrWarn(TAG, "Event[" + this.eventName + "] exceeds Firebase event name limit " + this.eventName.length() + " of " + MAX_LENGTH_EVENT_NAME);

            // fix the value if we just want to warn
            if (value != null) {
                int acceptValueLength = MAX_LENGTH_EVENT_NAME - eventNamePrefix.length();
                int valueLength = value.length();
                this.eventName = eventNamePrefix + value.substring(valueLength - acceptValueLength, valueLength);
            }
        }
    }

    @CheckResult
    public static FirebaseEvent create(@NonNull String category, @NonNull String method, @Nullable String object, String value) {
        return new FirebaseEvent(category, method, object, value);

    }

    public FirebaseEvent param(String name, String value) {
        if (this.eventParam == null) {
            this.eventParam = new Bundle();
        }
        // validate the size
        if (this.eventParam.size() >= MAX_PARAM_SIZE) {
            LoggerWrapper.throwOrWarn(TAG, "Firebase event[" + eventName + "] has too many parameters");
        }

        this.eventParam.putString(safeParamLength(name, MAX_LENGTH_PARAM_NAME),
                safeParamLength(value, MAX_LENGTH_PARAM_VALUE));

        return this;
    }

    // TODO: check param name should start with[a-zA-Z] , contains only [a-zA-A0-9_], and shouldn't
    // TODO: start with ^(?!(firebase_|google_|ga_)).*
    private static String safeParamLength(@NonNull final String str, final int end) {
        // validate the length
        if (str.length() > end) {
            LoggerWrapper.throwOrWarn(TAG, "Exceeding limit of param content length:" + str.length() + " of " + end);
        }
        // fix the value if we just want to warn
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
            FirebaseHelper.getFirebase().event(context.getApplicationContext(), this.eventName, this.eventParam);
        }
    }

    @VisibleForTesting
    public void setParam(Bundle bundle) {
        this.eventParam = bundle;
    }

    static String getValidPrefKey(@NonNull String value) {
        return prefKeyWhitelist.get(value);
    }

    /**
     * @return the whitelisted pref key. This may be empty if not initialized.
     */
    @VisibleForTesting
    static Map<String, String> getPrefKeyWhitelist() {
        return prefKeyWhitelist;
    }

    static boolean isInitialized() {
        return prefKeyWhitelist.size() != 0;
    }

    static void setPrefKeyWhitelist(HashMap<String, String> map) {
        prefKeyWhitelist = map;
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