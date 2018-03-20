/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.FirstrunFragment;

import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;
import okio.Okio;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.mozilla.focus.fragment.FirstrunFragment.PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN;
import static org.mozilla.focus.fragment.FirstrunFragment.PREF_KEY_INT_FIRSTRUN_UPGRADE_VERSION;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;

public final class AndroidTestUtils {

    public static void beforeTest() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (context == null) {
            return;
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, true)
                        .putInt(PREF_KEY_INT_FIRSTRUN_UPGRADE_VERSION, FirstrunFragment.FIRSTRUN_UPGRADE_VERSION).commit();
            }
        }
        final Settings settings = Settings.getInstance(context);
        if (settings != null) {
            settings.setShareAppDialogDidShow();
            settings.setRateAppDialogDidShow();
        }
    }

    public static Buffer readTestAsset(String filename) throws IOException {
        try (final InputStream stream = InstrumentationRegistry.getContext().getAssets().open(filename)) {
            return readStreamFile(stream);
        }
    }

    public static Buffer readStreamFile(InputStream file) throws IOException {
        final Buffer buffer = new Buffer();
        buffer.writeAll(Okio.source(file));
        return buffer;
    }

    public static void removeNewAddedTab() {
        onView(withId(R.id.counter_box)).perform(click());
        onView(withId(R.id.tab_tray)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.close_button)));
    }

    public static String getResourceId(String id) {
        return String.format("%s:id/%s", BuildConfig.APPLICATION_ID, id);
    }
}
