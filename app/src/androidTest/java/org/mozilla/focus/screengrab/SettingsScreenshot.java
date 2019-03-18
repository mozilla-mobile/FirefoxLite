/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.StorageUtils;

import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class SettingsScreenshot extends BaseScreenshot {

    private static final String TAG = "SettingsScreenshot";

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder().build().execute();
        activityTestRule.launchActivity(new Intent());
        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityTestRule.getActivity()));
    }

    @Test
    public void screenshotSettings() {

        Resources resources = activityTestRule.getActivity().getResources();

        // Home menu
        AndroidTestUtils.tapHomeMenuButton();

        onView(withId(R.id.menu_preferences)).perform(click());

        onView(allOf(withText(R.string.menu_settings), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_LIST_1);

        // Click language
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_locale))).perform(click());
        onView(withText(R.string.preference_language)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_LANGUAGE_1);
        Espresso.pressBack();

        // Click search engine
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_search_engine))).perform(click());
        onView(withText(R.string.preference_search_engine_default)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_SEARCH_ENGINE);
        Espresso.pressBack();

        // Click clear browser data
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_storage_clear_browsing_data))).perform(click());
        onView(withText(R.string.setting_dialog_clear_data)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_CLEAR_DATA);

        onView(withText(R.string.setting_dialog_clear_data)).inRoot(isDialog()).perform(click());
        onView(withText(R.string.message_cleared_browsing_data))
                .inRoot(withDecorView(not(is(activityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_CLEAR_DATA_TOAST);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        try {
            StorageUtils.getTargetDirOnRemovableStorageForDownloads(activityTestRule.getActivity(), "*/*");

            onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_storage_save_downloads_to))).perform(click());
            onView(withText(R.string.preference_privacy_storage_save_downloads_to)).inRoot(isDialog()).check(matches(isDisplayed()));
            Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_SAVE_DOWNLOADS);
            Espresso.pressBack();
        } catch (NoRemovableStorageException e) {
            Log.e(TAG, "there's no removable storage for screenshot so we skip this.");
        }

        // Click give feedback
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_give_feedback))).perform(click());
        onView(withId(R.id.dialog_rate_app_btn_feedback)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_FEEDBACK);
        Espresso.pressBack();

        // Click share with friends
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_share_with_friends))).perform(click());
        onView(withText(R.string.share_app_dialog_btn_share)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_SHARE);
        // TODO ScreenshotNamingUtils.SETTINGS_SHARE_GMAIL
        Espresso.pressBack();

        // Click about
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_about))).perform(click());
        onView(withText(R.string.menu_about)).check(matches(isDisplayed()));
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_ABOUT);

        // Click your rights/licenses on about page
        onWebView().withElement(findElement(Locator.XPATH, String.format("//a[contains(text(),'%s')]", resources.getString(R.string.about_link_your_rights)))).perform(webClick());
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_ABOUT_YOUR_RIGHT);
        Espresso.pressBack();
        Espresso.pressBack();

        Screengrab.screenshot(ScreenshotNamingUtils.SETTINGS_LIST_2);

    }

}
