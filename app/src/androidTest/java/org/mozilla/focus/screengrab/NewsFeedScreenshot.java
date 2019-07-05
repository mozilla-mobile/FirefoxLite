/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.content.Intent;
import android.support.test.espresso.Espresso;
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
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class NewsFeedScreenshot extends BaseScreenshot {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder()
                .enableLifeFeedNews(true)
                .build()
                .execute();
        activityTestRule.launchActivity(new Intent());
        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityTestRule.getActivity()));
    }

    @Test
    public void screenshotNewsFeed() {

        // Scroll up to see news feed
        // Note: it needs to tap arrow and menu twice to bring up news feed
        AndroidTestUtils.tapHomeMenuButton();
        Espresso.pressBack();
        onView(allOf(withId(R.id.arrow_container), isDisplayed())).perform(click());
        AndroidTestUtils.tapHomeMenuButton();
        Espresso.pressBack();
        onView(allOf(withId(R.id.arrow_container), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.NEWS_LIST);

        // Tap news setting
        onView(allOf(withId(R.id.news_setting), isDisplayed())).perform(click());
        Screengrab.screenshot(ScreenshotNamingUtils.NEWS_SETTINGS);
    }
}