/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;

import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class OnboardingScreenshot extends BaseScreenshot {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder()
                .setSkipFirstRun(false)
                .setSkipColorThemeOnBoarding(false)
                .build()
                .execute();
        activityTestRule.launchActivity(new Intent());
        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityTestRule.getActivity()));
    }
    @Test
    public void screenshotOnboardingPage() {

        // Check if turbo mode switch is on
        onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()));
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_1);

        // Click next button in the first on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_2);

        // Click next button in the second on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_3);

        // Click next button in the third on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_4);

        // Click finish button to finish on boarding
        onView(allOf(withId(R.id.finish), isDisplayed())).perform(click());

        onView(withId(R.id.fragment_homescreen_theme_onboarding)).check(matches(isDisplayed()));

        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_COLOR_THEME);

        onView(withId(R.id.fragment_homescreen_theme_onboarding)).perform(click());

        // Show my shot onboarding view
        activityTestRule.getActivity().runOnUiThread(activityTestRule.getActivity()::showMyShotOnBoarding);

        onView(withText(R.string.my_shot_on_boarding_message)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_MY_SHOT);
        // Dismiss my shot onboarding view
        onView(withText(R.string.my_shot_on_boarding_message)).inRoot(isDialog()).perform(click());

        // Turn on night mode
        onView(withId(R.id.menu_night_mode)).perform(click());

        // Dismiss adjust brightness dialog and open menu
        onView(withId(R.id.brightness_root)).perform(click());
        AndroidTestUtils.tapHomeMenuButton();

        onView(withText(R.string.night_mode_on_boarding_message)).inRoot(isDialog()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.ONBOARDING_NIGHT_MODE);
        onView(withText(R.string.night_mode_on_boarding_message)).inRoot(isDialog()).perform(click());

    }
}