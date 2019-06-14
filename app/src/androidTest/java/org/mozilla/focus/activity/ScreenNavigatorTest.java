/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import androidx.annotation.Keep;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@Keep
@RunWith(AndroidJUnit4.class)
public class ScreenNavigatorTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    private static final String TARGET_URL_SITE_1 = "file:///android_asset/gpl.html";

    private MainActivity activity;

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
        activity = activityTestRule.launchActivity(new Intent());
    }

    /**
     * Test whether showBrowserScreen() can load and bring browser screen to foreground properly
     */
    @Test
    public void launch_showBrowserScreen_browserScreenDisplayed() {
        ScreenNavigatorWrapper navigator = new ScreenNavigatorWrapper(activity);
        assertNewPageLoaded(navigator, TARGET_URL_SITE_1);
    }

    /**
     * Simulating the below UI behavior
     * 1. Browsing
     * 2. Press new tab on tool bar
     * 3. Open tab tray
     * 4. Press new tab on tab tray
     * 5. Repeat step 3~4
     * 6. Press back
     * 7. There should be only 1 home screen, and pressing back will lead to previous browsing page
     */
    @Test
    public void browsing_addHomeAndBack_backToBrowsing() {
        ScreenNavigatorWrapper navigator = new ScreenNavigatorWrapper(activity);

        // Prepare
        assertNewPageLoaded(navigator, TARGET_URL_SITE_1);

        // Add multiple home
        navigator.addHomeScreen();
        navigator.addHomeScreen();
        onView(withId(R.id.home_container)).check(matches(isDisplayed()));

        // Back
        Espresso.pressBack();

        // Should go back to original browser screen
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));
    }

    /**
     * Simulating the below UI behavior
     * 1. Browsing
     * 2. Press new tab on tool bar
     * 3. Open tab tray
     * 4. Close all tab (this will call ScreenNavigator#popToHome())
     * 7. There should be only 1 home screen, and pressing back will finish the app
     */
    @Test(expected = NoActivityResumedException.class)
    public void addHome_popToHomeAndBack_activityFinished() {
        ScreenNavigatorWrapper navigator = new ScreenNavigatorWrapper(activity);

        // Prepare
        assertNewPageLoaded(navigator, TARGET_URL_SITE_1);

        // Prepare
        navigator.addHomeScreen();
        onView(withId(R.id.home_fragment_fake_input)).check(matches(isDisplayed()));

        // Pop to home
        navigator.popToHomeScreen();

        // Back should cause exception
        Espresso.pressBack();
    }

    private void assertNewPageLoaded(ScreenNavigatorWrapper navigator, String url) {
        final SessionLoadedIdlingResource loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        navigator.showBrowserScreen(url);

        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(url)));
        Assert.assertEquals(true, navigator.isBrowserInForeground());
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);
    }

    private static class ScreenNavigatorWrapper {
        private MainActivity activity;

        ScreenNavigatorWrapper(MainActivity activity) {
            this.activity = activity;
        }

        void showBrowserScreen(final String url) {
            activity.runOnUiThread(() -> activity.getScreenNavigator().showBrowserScreen(url,
                    true, false));
        }

        void addHomeScreen() {
            activity.runOnUiThread(() -> activity.getScreenNavigator().addHomeScreen(false));
        }

        void popToHomeScreen() {
            activity.runOnUiThread(() -> activity.getScreenNavigator().popToHomeScreen(false));
        }

        boolean isBrowserInForeground() {
            return activity.getScreenNavigator().isBrowserInForeground();
        }
    }
}