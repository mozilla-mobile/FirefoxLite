/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.view.KeyEvent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.autobot.BottomBarRobotKt;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.BottomBarViewModel;
import org.mozilla.rocket.chrome.MenuViewModel;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class NavigationTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private static final String TARGET_URL_SITE_1 = "file:///android_asset/gpl.html";
    private static final String TARGET_URL_SITE_2 = "file:///android_asset/licenses.html";

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
        activityTestRule.launchActivity(new Intent());
    }

    /**
     * Test case no: TC_0013
     * Test case name: Navigation and back and forth to previous site
     * Steps:
     * 1. Launch Rocket
     * 2. Visit a website_1
     * 3. Visit a website_2
     * 4. Press Back button
     * 5. check website_1 is displayed
     * 6. open menu to click next button
     * 7. check website_2 is displayed
     */
    @Test
    public void browsingWebsiteBackAndForward_backAndFrowardToWebsite() {

        final SessionLoadedIdlingResource loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Click search field
        onView(withId(R.id.home_fragment_fake_input))
                .perform(click());

        // Enter site 1 url
        onView(withId(R.id.url_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.url_edit)).perform(typeText(TARGET_URL_SITE_1));
        onView(withId(R.id.url_edit)).perform(pressKey(KeyEvent.KEYCODE_ENTER));

        // Check if site 1 url is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Click search button and clear existing text in search field
        int bottomBarSearchPos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_SEARCH);
        new BottomBarRobot().clickBrowserBottomBarItem(bottomBarSearchPos);
        onView(withId(R.id.url_edit)).perform(clearText());

        // Enter site 2 url
        onView(withId(R.id.url_edit)).perform(typeText(TARGET_URL_SITE_2));
        onView(withId(R.id.url_edit)).perform(pressKey(KeyEvent.KEYCODE_ENTER));

        // Check if site 2 url is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_2)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Press back and check if go back to site 1
        Espresso.pressBack();
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Open menu and click next button
        AndroidTestUtils.tapBrowserMenuButton();
        int menuBottomBarNextPos = BottomBarRobotKt.indexOfType(MenuViewModel.getDEFAULT_MENU_BOTTOM_ITEMS(), BottomBarItemAdapter.TYPE_NEXT);
        new BottomBarRobot().clickMenuBottomBarItem(menuBottomBarNextPos);

        // Check if site 2 is loaded again
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_2)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Press back and check if site 1 is loading again
        Espresso.pressBack();
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

    }

}