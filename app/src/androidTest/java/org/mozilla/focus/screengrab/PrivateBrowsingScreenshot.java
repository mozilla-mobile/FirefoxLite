/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.PrivateSessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.rocket.content.ExtentionKt;
import org.mozilla.rocket.privately.PrivateModeActivity;

import mozilla.components.browser.session.SessionManager;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class PrivateBrowsingScreenshot extends BaseScreenshot {

    private static final String TARGET_URL_SITE = "file:///android_asset/gpl.html";
    private static final int NOTIFICATION_DISPLAY_DELAY = 2000;

    private final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private PrivateSessionLoadedIdlingResource loadingIdlingResource;

    @Rule
    public ActivityTestRule<PrivateModeActivity> activityTestRule = new ActivityTestRule<>(PrivateModeActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder().build().execute();
        activityTestRule.launchActivity(new Intent());
        final NotificationManager notificationManager = (NotificationManager) activityTestRule.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @After
    public void tearDown() {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
    }

    @Test
    public void screenshotPrivateBrowsing() {

        // Click private home search field and enter test url
        onView(withId(R.id.pm_home_fake_input)).perform(click());

        // create the idlingResource before the new session is created.
        SessionManager sessionManager = ExtentionKt.app(activityTestRule.getActivity()).getSessionManager();
        loadingIdlingResource = new PrivateSessionLoadedIdlingResource(sessionManager, "screenshotPrivateBrowsing");

        onView(withId(R.id.url_edit)).perform(replaceText(TARGET_URL_SITE), pressImeActionButton());

        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Once the new session is created, we listen when the session completes loading
        IdlingRegistry.getInstance().register(loadingIdlingResource);

        onView(withId(R.id.display_url)).check(matches(withText(TARGET_URL_SITE)));

        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Finish private browsing
        new BottomBarRobot().clickBrowserBottomBarItem(R.id.bottom_bar_delete);

        // Check private browsing is cleared toast is displayed
        MockUIUtils.showToast(activityTestRule.getActivity(), R.string.private_browsing_erase_done);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.private_browsing_erase_done);

        // Take screenshot
        Screengrab.screenshot(ScreenshotNamingUtils.PRIVATE_ERASING_TOAST);

        // Click private home search field and enter test url again
        onView(withId(R.id.pm_home_fake_input)).perform(click());
        onView(withId(R.id.url_edit)).perform(replaceText(TARGET_URL_SITE), pressImeActionButton());
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Check if test url is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(withText(TARGET_URL_SITE)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Expand status bar
        uiDevice.openNotification();

        // Check if erasing private browsing notification is displayed
        String notificationText = activityTestRule.getActivity().getResources().getString(R.string.private_browsing_erase_message);
        UiObject notificationObject = uiDevice.findObject(new UiSelector().text(notificationText));
        notificationObject.waitForExists(NOTIFICATION_DISPLAY_DELAY);

        // Take screenshot
        Screengrab.screenshot(ScreenshotNamingUtils.PRIVATE_ERASING_NOTIFICATION);

        // Click notification to close status bar
        try {
            notificationObject.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

    }

}
