/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@Keep
@RunWith(AndroidJUnit4.class)
public class MenuTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();
    }

    /**
     * Test case no: TC0034
     * Test case name: menu layout
     * Steps:
     * 1. Launch Rocket
     * 2. check visible -
     * - first row : Bookmarks, Downloads, History, My shots
     * - second row : Turbo mode (default enabled), Private browsing, Night mode, Block images
     * - Third row : Find in page, Clear cache, Settings, Exit
     */
    @Test
    public void checkMenuLayout() {

        activityRule.launchActivity(new Intent());

        // Open menu
        AndroidTestUtils.tapHomeMenuButton();

        // check downloads,
        onView(withId(R.id.menu_download)).check(matches(isDisplayed()));

        // check bookmarks
        onView(withId(R.id.menu_bookmark)).check(matches(isDisplayed()));

        // check history
        onView(withId(R.id.menu_history)).check(matches(isDisplayed()));

        // check my shots
        onView(withId(R.id.menu_screenshots)).check(matches(isDisplayed()));

        // check turbo mode displayed, enabled
        onView(withId(R.id.menu_turbomode)).check(matches(isEnabled()));

        onView(withId(R.id.menu_turbomode)).check(matches(isDisplayed()));

        // check private mode displayed
        onView(withId(R.id.btn_private_browsing)).check(matches(isDisplayed()));

        // check night mode displayed
        onView(withId(R.id.menu_night_mode)).check(matches(isDisplayed()));

        // check block img displayed
        onView(withId(R.id.menu_blockimg)).check(matches(isDisplayed()));

        // check find in page displayed
        onView(withId(R.id.menu_find_in_page)).check(matches(isDisplayed()));

        // check clear cached displayed
        onView(withId(R.id.menu_delete)).check(matches(isDisplayed()));

        // check settings displayed
        onView(withId(R.id.menu_preferences)).check(matches(isDisplayed()));

        // check exit displayed
        onView(withId(R.id.menu_exit)).check(matches(isDisplayed()));
    }
}