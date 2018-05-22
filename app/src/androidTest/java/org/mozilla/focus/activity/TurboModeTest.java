/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class TurboModeTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest(false);
        activityTestRule.launchActivity(new Intent());
    }

    @Test
    public void turnOnTurboModeDuringOnBoarding_turboModeIsOnInMenu() {

        // Check if turbo mode switch is on
        onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()));

        // Click next button in the first on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Click next button in the second on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Click next button in the third on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Click finish button to finish on boarding
        onView(allOf(withId(R.id.finish), isDisplayed())).perform(click());

        // Open home menu
        onView(allOf(withId(R.id.btn_menu), withParent(withId(R.id.home_screen_menu)))).perform(click());

        // Check if turbo mode is on
        onView(withId(R.id.menu_turbomode)).check(matches(isDisplayed())).check(matches(isSelected()));

        // Close menu
        Espresso.pressBack();

    }

}