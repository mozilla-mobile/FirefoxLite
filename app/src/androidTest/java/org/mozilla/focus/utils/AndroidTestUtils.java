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
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.FirstrunFragment;

import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;
import okio.Okio;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.mozilla.focus.fragment.FirstrunFragment.PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN;
import static org.mozilla.focus.fragment.FirstrunFragment.PREF_KEY_INT_FIRSTRUN_UPGRADE_VERSION;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.EspressoUtils.PREF_KEY_ENABLE_GEOLOCATION_PERMISSION_PROMPT;
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

    public static void setAllowGeoPermissionPrompt(final boolean allow) {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            if (editor != null) {
                editor.putBoolean(PREF_KEY_ENABLE_GEOLOCATION_PERMISSION_PROMPT, allow).commit();
            }
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

    public static void tapHomeMenuButton() {
        onView(allOf(withId(R.id.btn_menu), withParent(withId(R.id.home_screen_menu)))).perform(click());
    }

    public static void tapBrowserMenuButton() {
        onView(allOf(withId(R.id.btn_menu), not(withParent(withId(R.id.home_screen_menu))))).perform(click());
    }

    public static void tapSettingButton() {
        onView(allOf(withId(R.id.menu_preferences), isDisplayed())).perform(click());
    }

    public static void tapHomeSearchField() {
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());
    }

    public static void typeTextInSearchFieldAndGo(String text) {
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(text), pressImeActionButton());
    }

    public static void urlBarContainsText(String text) {
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(containsString(text))));
    }

}
