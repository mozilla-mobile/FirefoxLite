/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.widget.FragmentListener;

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
        beforeTest(true);
    }

    public static void beforeTest(final boolean firstRun) {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (context == null) {
            return;
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, firstRun)
                        .putInt(PREF_KEY_INT_FIRSTRUN_UPGRADE_VERSION, FirstrunFragment.FIRSTRUN_UPGRADE_VERSION).commit();
            }
        }
        final Settings settings = Settings.getInstance(context);
        if (settings != null) {
            settings.setShareAppDialogDidShow();
            settings.setRateAppDialogDidShow();
        }

        Inject.getTabsDatabase(null).tabDao().deleteAllTabs();
        setFocusTabId("");
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

    public static void setFocusTabId(String focusTabId) {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.putString("pref_key_focus_tab_id", focusTabId).commit();
            }
        }
    }

    public static ViewAction clickXY(final int x, final int y, final Tap tap) {
        return new GeneralClickAction(
                tap,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {

                        final int[] screenPos = new int[2];
                        view.getLocationOnScreen(screenPos);

                        final float screenX = screenPos[0] + x;
                        final float screenY = screenPos[1] + y;
                        float[] coordinates = {screenX, screenY};

                        return coordinates;
                    }
                }, Press.FINGER, 0, 0, null);
    }

    public static void showHomeMenu(ActivityTestRule<MainActivity> activityTestRule) {
        if (activityTestRule != null) {
            final MainActivity mainActivity = activityTestRule.getActivity();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.onNotified(null, FragmentListener.TYPE.SHOW_MENU, null);
                }
            });
        }
    }

}
