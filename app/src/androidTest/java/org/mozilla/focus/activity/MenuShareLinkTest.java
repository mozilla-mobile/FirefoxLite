/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.autobot.BottomBarRobotKt;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.MenuViewModel;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;


@RunWith(AndroidJUnit4.class)
public class MenuShareLinkTest {

    private static final String TEST_PATH = "/";
    private static final String IMAGE_FILE_NAME_DOWNLOADED = "rabbit.jpg";
    private static final String HTML_FILE_FULL_SCREEN_IMAGE = "fullscreen_image_test.html";

    private MockWebServer webServer;
    private SessionLoadedIdlingResource sessionLoadedIdlingResource;

    @Rule
    public final GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public final GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule = new IntentsTestRule<MainActivity>(MainActivity.class, true, false) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_FULL_SCREEN_IMAGE))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_DOWNLOADED)));
                webServer.start();
            } catch (IOException e) {
                throw new AssertionError("Could not start web server", e);
            }
        }

        @Override
        protected void afterActivityFinished() {
            super.afterActivityFinished();
            try {
                webServer.close();
                webServer.shutdown();
            } catch (IOException e) {
                throw new AssertionError("Could not stop web server", e);
            }
        }
    };

    @Before
    public void setUp() {
        new BeforeTestTask.Builder()
                .build()
                .execute();
        intentsTestRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() {
        if (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
    }

    /**
     * Test case no: TC0040
     * Test case name: Share link in menu
     * Steps:
     * 1. Launch app, visit a website, and tap menu -> share link
     * 2. Check intent sent
     */
    @Test
    public void shareLinkInMenu() {

        // Launch Rocket and visit a website
        loadTestWebsiteAndShareLinkInMenu();

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Check intent sent
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent.ACTION_SEND), hasExtra(Intent.EXTRA_TEXT, getLinkUrl())))));
    }

    /**
     * Test case no: TC0037
     * Test case name: Share btn disabled on home page
     * Steps:
     * 1. Launch app
     * 2. Tap menu
     * 3. Check share btn disabled
     */
    @Test
    public void shareLinkDisabledOnHomePage() {

        // Tap menu
        AndroidTestUtils.tapHomeMenuButton();

        // Check share btn disabled
        int menuBottomBarSharePos = BottomBarRobotKt.indexOfType(MenuViewModel.getDEFAULT_MENU_BOTTOM_ITEMS(), BottomBarItemAdapter.TYPE_SHARE);
        onView(new BottomBarRobot().menuBottomBarItemView(menuBottomBarSharePos)).check(matches(not(isEnabled())));
    }


    private void loadTestWebsiteAndShareLinkInMenu() {
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(intentsTestRule.getActivity());
        // Click home search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        // Waiting for the page is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);

        // Tap menu -> share link
        AndroidTestUtils.tapBrowserMenuButton();
        int menuBottomBarSharePos = BottomBarRobotKt.indexOfType(MenuViewModel.getDEFAULT_MENU_BOTTOM_ITEMS(), BottomBarItemAdapter.TYPE_SHARE);
        onView(new BottomBarRobot().menuBottomBarItemView(menuBottomBarSharePos)).check(matches(isDisplayed())).perform(click());

        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
    }

    private String getLinkUrl() {
        return webServer.url(TEST_PATH).toString();
    }

}
