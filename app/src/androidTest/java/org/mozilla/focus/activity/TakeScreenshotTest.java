/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.espresso.Espresso;
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
import org.mozilla.focus.helper.ScreenshotIdlingResource;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class TakeScreenshotTest {

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
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() {
        if (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
        if (screenshotIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(screenshotIdlingResource);
        }
        activityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void takeScreenshot_screenshotIsCaptured () {

        activityTestRule.launchActivity(new Intent());

        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Click search field
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());

        // Enter test site url
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TARGET_URL_SITE), pressImeActionButton());

        // Check if test site is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(TARGET_URL_SITE)));
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        screenshotIdlingResource = new ScreenshotIdlingResource();

        // Click screen capture button
        onView(allOf(withId(R.id.btn_capture), isDisplayed())).perform(click());

        // Register screenshot taken idling resource and wait capture complete
        IdlingRegistry.getInstance().register(screenshotIdlingResource);

        // wait for the screen shot to complete
        // Open menu
        onView(allOf(withId(R.id.btn_menu), isDisplayed())).perform(click());

        IdlingRegistry.getInstance().unregister(screenshotIdlingResource);

        // Click my shot
        onView(allOf(withId(R.id.menu_screenshots), isDisplayed())).perform(click());

        // Click the first item in my shots panel
        // Since "index=0" in ScreenshotItemAdapter is always date label, the first screenshot item will start from "index=1".
        onView(withId(R.id.screenshot_grid_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Check if screenshot is displayed
        onView(withId(R.id.screenshot_viewer_image)).check(matches(isDisplayed()));

        // Check if open url/edit/share/info/delete button is there
        onView(withId(R.id.screenshot_viewer_btn_open_url)).check(matches(isDisplayed()));
        onView(withId(R.id.screenshot_viewer_btn_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.screenshot_viewer_btn_share)).check(matches(isDisplayed()));
        onView(withId(R.id.screenshot_viewer_btn_info)).check(matches(isDisplayed()));
        onView(withId(R.id.screenshot_viewer_btn_delete)).check(matches(isDisplayed()));

        // Delete the screenshot
        onView(withId(R.id.screenshot_viewer_btn_delete)).perform(click());

        // Confirm delete
        onView(allOf(withText(R.string.browsing_history_menu_delete), isDisplayed())).perform(click());

        // Check if come back to my shots panel
        onView(withId(R.id.screenshots)).check(matches(isDisplayed()));

        // Back to home
        Espresso.pressBack();
    }
}