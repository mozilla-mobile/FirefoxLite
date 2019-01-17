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
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class PrivateBrowsingTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();
        activityRule.launchActivity(new Intent());
    }

    /**
     * Test case no: TC0108
     * Test case name: Open private mode and back when browser tab number is 0
     * Steps:
     * 1. Launch Rocket
     * 2. Tap menu
     * 3. Tap private mode
     * 4. Tap private button to be back normal mode
     * 5. Check tab tray number is 0
     */
    @Test
    public void openPrivateModeAndBack_whenBrowserTabNumberZero() {

        // Tap menu
        AndroidTestUtils.tapBrowserMenuButton();

        // Tap private mode
        AndroidTestUtils.tapPrivateButtonInMenu();

        // Tap private button to be back normal mode
        AndroidTestUtils.tapPrivateButtonBackToBrowser();

        // Check tab tray number is 0
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.home_screen_menu)))).check(matches(withText("0")));
    }
}