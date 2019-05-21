/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.Manifest;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.autobot.BottomBarRobotKt;
import org.mozilla.focus.fragment.ScreenCaptureDialogFragment;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.ScreenshotIdlingResource;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.BottomBarViewModel;

import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class MyShotScreenshot extends BaseScreenshot {

    private SessionLoadedIdlingResource sessionLoadedIdlingResource;
    private ScreenshotIdlingResource screenshotIdlingResource;

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Rule
    public final GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public final GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    private static final String TARGET_URL_SITE = "file:///android_asset/gpl.html";

    @Before
    public void setUp() {
        new BeforeTestTask.Builder().build().execute();
        activityTestRule.launchActivity(new Intent());
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());
        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityTestRule.getActivity()));
    }

    @After
    public void tearDown() {
        if (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
        if (screenshotIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(screenshotIdlingResource);
        }
    }

    @Test
    public void screenshotMyShot() {

        // Browsing a web page
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TARGET_URL_SITE), pressImeActionButton());

        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(TARGET_URL_SITE)));
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        MockUIUtils.showToast(activityTestRule.getActivity(), R.string.screenshot_failed);
        AndroidTestUtils.toastContainsText(activityTestRule.getActivity(), R.string.screenshot_failed);
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_FAILED_TO_CAPTURE);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);


        final ScreenCaptureDialogFragment capturingFragment = ScreenCaptureDialogFragment.newInstance();
        capturingFragment.show(activityTestRule.getActivity().getBrowserFragment().getChildFragmentManager(), "capturingFragment");
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);
        onView(withText(R.string.screenshot_illustration)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_CAPTURING);
        capturingFragment.dismiss(false);

        screenshotIdlingResource = new ScreenshotIdlingResource(activityTestRule.getActivity());

        // Click screen capture button
        int bottomBarCapturePos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_CAPTURE);
        new BottomBarRobot().clickBrowserBottomBarItem(bottomBarCapturePos);
        IdlingRegistry.getInstance().register(screenshotIdlingResource);

        // Open menu
        AndroidTestUtils.tapHomeMenuButton();
        IdlingRegistry.getInstance().unregister(screenshotIdlingResource);

        // Click my shot and take a screenshot of panel and toast
        onView(allOf(withId(R.id.menu_screenshots), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_PANEL_AND_SAVED);

        // Click the first item in my shots panel
        // Since "index=0" in ScreenshotItemAdapter is always date label, the first screenshot item will start from "index=1".
        onView(withId(R.id.screenshot_grid_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Check if screenshot is displayed
        onView(withId(R.id.screenshot_viewer_image)).check(matches(isDisplayed()));

        // Click the info button and take a screenshot
        onView(withId(R.id.screenshot_viewer_btn_info)).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_INFO);

        onView(withText(R.string.action_ok)).inRoot(isDialog()).perform(click());

        // Click the delete button and take a screenshot
        onView(withId(R.id.screenshot_viewer_btn_delete)).perform(click());
        onView(withText(R.string.browsing_history_menu_delete)).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_DELETE);

        // Click delete button and take a screenshot of toast
        onView(allOf(withText(R.string.browsing_history_menu_delete), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.SCREENSHOT_DELETED);
        onView(withId(R.id.screenshots)).check(matches(isDisplayed()));

    }
}