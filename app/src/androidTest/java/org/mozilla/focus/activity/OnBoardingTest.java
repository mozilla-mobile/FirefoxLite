/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import androidx.annotation.Keep;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.filters.FlakyTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.ViewPagerIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class OnBoardingTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);
    private ViewPagerIdlingResource viewPagerIdlingResource;

    @Before
    public void setUp() {
        new BeforeTestTask.Builder()
                .setSkipFirstRun(false)
                .build()
                .execute();
        activityTestRule.launchActivity(new Intent());
    }

    /**
     * Test case no: TC_0033
     * Test case name: Go through onboarding
     * Steps:
     * 1. Launch Rocket
     * 2. check turbo mode is selected on first onbording page
     * 3. move forward and backfard to check 4 pages of onboarding are displayed correctly
     * 4. open menu
     * 5. check turbo mode is selected
     */
    @FlakyTest
    @Test
    public void turnOnTurboModeDuringOnBoarding_turboModeIsOnInMenu() {

        // Check if turbo mode switch is on
        onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()));

        // Click next button in the first on-boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Click next button in the second on-boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Register view pager idling resource
        viewPagerIdlingResource = new ViewPagerIdlingResource(activityTestRule.getActivity().findViewById(R.id.pager));

        IdlingRegistry.getInstance().register(viewPagerIdlingResource);

        // Swipe right to go to second on-boarding page
        onView(allOf(withId(R.id.image), isDisplayed())).perform(swipeRight());

        // Swipe left to go to to third on-boarding page
        onView(allOf(withId(R.id.image), isDisplayed())).perform(swipeLeft());

        // Unregister view pager idling resource
        IdlingRegistry.getInstance().unregister(viewPagerIdlingResource);

        // Click next button in the third on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());

        // Click finish button to finish on boarding
        onView(allOf(withId(R.id.finish), isDisplayed())).perform(click());

        // Open home menu
        AndroidTestUtils.tapHomeMenuButton();

        // Check if turbo mode is on
        onView(withId(R.id.menu_turbomode))
                .inRoot(RootMatchers.isDialog())
                .check(matches(isDisplayed()))
                .check(matches(isSelected()));

        // Close menu
        Espresso.pressBack();

    }

}