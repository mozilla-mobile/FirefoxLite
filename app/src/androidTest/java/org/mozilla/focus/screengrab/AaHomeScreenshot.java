/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.DialogUtils;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
/**
 * Add 'Aa' prefix in class name to make sure this test is the first one to run
 **/
public class AaHomeScreenshot extends BaseScreenshot {

    UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder().build().execute();
        activityTestRule.launchActivity(new Intent());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
//        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityTestRule.getActivity()));

    }

    @Test
    public void screenshotHomeFragment() {

        // Remove top site button
        onView(withId(R.id.main_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_REMOVE_TOP_SITE);
        Espresso.pressBack();

        // Home menu
        AndroidTestUtils.tapHomeMenuButton();
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU);

        // Click bookmarks
        onView(withId(R.id.menu_bookmark)).perform(click());
        onView(withText(R.string.bookmarks_empty_view_msg)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_NO_BOOKMARKS);

        // Click downloads
        onView(withId(R.id.downloads)).perform(click());
        onView(withText(R.string.no_downloads)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_NO_DOWNLOAD);

        // Click history
        onView(withId(R.id.history)).perform(click());
        onView(withText(R.string.browsing_history_empty_view_msg)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_NO_HISTORY);

        // Click screenshot
        onView(withId(R.id.screenshots)).perform(click());
        onView(withText(R.string.screenshot_grid_empty_text_title)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_NO_SCREENSHOT);

        Espresso.pressBack();
        AndroidTestUtils.tapHomeMenuButton();

        // Disable turbo mode
        onView(withId(R.id.menu_turbomode)).perform(click());
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.message_disable_turbo_mode);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_TURBO_MODE_DISABLED);

        // Disable turbo mode
        AndroidTestUtils.tapHomeMenuButton();
        onView(withId(R.id.menu_turbomode)).perform(click());
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.message_enable_turbo_mode);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_TURBO_MODE_ENABLED);

        // Enable block image
        AndroidTestUtils.tapHomeMenuButton();
        onView(withId(R.id.menu_blockimg)).perform(click());
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.message_enable_block_image);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_BLOCK_IMG_ENABLED);

        // Disable turbo mode
        AndroidTestUtils.tapHomeMenuButton();
        onView(withId(R.id.menu_blockimg)).perform(click());
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.message_disable_block_image);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_BLOCK_IMG_DISABLED);

        // Clear cache
        AndroidTestUtils.tapHomeMenuButton();
        onView(withId(R.id.menu_delete)).perform(click());

        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_MENU_CLEAR_CACHE);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);


        Resources resources = activityTestRule.getActivity().getResources();

        activityTestRule.getActivity().runOnUiThread(() -> DialogUtils.showRateAppNotification(activityTestRule.getActivity()));
        device.wait(Until.hasObject(By.text(resources.getString(R.string.rate_app_dialog_text_title, resources.getString(R.string.app_name)) + "\uD83D\uDE00")), MockUIUtils.LONG_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_NOTI_RATE_APP);

        activityTestRule.getActivity().runOnUiThread(() -> DialogUtils.showDefaultSettingNotification(activityTestRule.getActivity()));
        device.wait(Until.hasObject(By.text(resources.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A")), MockUIUtils.LONG_DELAY);
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_NOTI_DEFAULT_SETTING);

        activityTestRule.getActivity().runOnUiThread(() -> DialogUtils.showPrivacyPolicyUpdateNotification(activityTestRule.getActivity()));
        device.wait(Until.hasObject(By.text(resources.getString(R.string.privacy_policy_update_notification_title))), MockUIUtils.LONG_DELAY);
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.HOME_NOTI_PRIVACY_UPDATE);

    }

}
