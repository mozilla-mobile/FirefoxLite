/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;
import android.view.View;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.helper.BeforeTestTask;

import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;
import okio.Okio;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;

public final class AndroidTestUtils {

    public static void beforeTest() {
        new BeforeTestTask.Builder()
                .build()
                .execute();
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

    public static void setRateAppPromotionIsReadyToShow() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Clear rate app did show flag
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(context.getResources().getString(R.string.pref_key_did_show_rate_app_dialog), false).apply();

        // Force app created count to hit threshold to show app promotion dialog
        final Settings.EventHistory history = Settings.getInstance(context).getEventHistory();
        history.setCount(Settings.Event.AppCreate, (int) AppConfigWrapper.getRateDialogLaunchTimeThreshold(context));
    }
}
