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
import org.mozilla.focus.persistence.TabEntity;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class PrivateBrowsingTest {

    private static final TabEntity TAB = new TabEntity("TEST_ID", "ID_HOME", "Yahoo TW", "https://tw.yahoo.com/");
    private static final TabEntity TAB_2 = new TabEntity("TEST_ID_2", TAB.getId(), "Google", "https://www.google.com/");

    private TabsDatabase tabsDatabase;

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();
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

        // Start activity
        activityRule.launchActivity(new Intent());

        // Tap menu
        AndroidTestUtils.tapBrowserMenuButton();

        // Tap private mode
        AndroidTestUtils.tapPrivateButtonInMenu();

        // Tap private button to be back normal mode
        AndroidTestUtils.tapPrivateButtonBackToBrowser();

        // Check tab tray number is 0
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.home_screen_menu)))).check(matches(withText("0")));
    }

    /**
     * Test case no: TC0109
     * Test case name: Open private mode and back when browser tab number is over 0
     * Steps:
     * 1. Launch Rocket with tab number is 2 (by searching site_1 and then site2)
     * 2. Tap menu -> private mode
     * 3. Tap private button to be back normal mode
     * 4. Check tab number is 2
     * 5. Tap tab tray number
     * 6. Check top item (site_2) focused
     * 7. Tab private mode in tab tray
     * 8. Tab system back key
     * 9. Check top item (site_2) focused
     */
    @Test
    public void openPrivateModeAndBack_whenBrowserTabNumbeOverZero() {

        // Launch Rocket with tab number is 2 (by searching site_1 and then site2)
        tabsDatabase.tabDao().insertTabs(TAB_2, TAB);
        AndroidTestUtils.setFocusTabId(TAB_2.getId());
        activityRule.launchActivity(new Intent());

        // Tap menu -> private mode
        AndroidTestUtils.tapBrowserMenuButton();
        AndroidTestUtils.tapPrivateButtonInMenu();

        // Tap private button to be back normal mode
        AndroidTestUtils.tapPrivateButtonBackToBrowser();

        // Check tab number is 2 and tap it
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.browser_screen_menu)))).check(matches(withText("2"))).perform(click());

        // Check top item (site_2) focused
        onView(withId(R.id.recyclerview)).check(matches(atPosition(0, hasDescendant(withText(TAB_2.getUrl()))))).check(matches(isSelected()));
        // Tab private mode in tab tray
        onView(allOf(withId(R.id.btn_private_browsing), isDisplayed())).perform(click());

        // Tab system back key
        pressBack();

        // Check top item (site_2) focused
        onView(withId(R.id.recyclerview)).check(matches(atPosition(0, hasDescendant(withText(TAB_2.getUrl()))))).check(matches(isSelected()));
    }
}
