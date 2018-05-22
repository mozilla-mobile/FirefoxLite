/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class BrowsingHistoryTest {

    private static final String TEST_PATH = "/";
    private static final String HTML_FILE_GET_LOCATION = "get_location.html";

    private MockWebServer webServer;

    private SessionLoadedIdlingResource loadingIdlingResource;

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class, true, false) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            webServer = new MockWebServer();
            try {
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_GET_LOCATION))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_GET_LOCATION))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
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
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() {
        // We unregister loadingIdlingResource here so other tests will not be affected.
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }


    }

    @Test
    public void browsingWebsiteThenTapTheFirstHistoryItem_browsingHistoryRecordCorrectly() {

        activityTestRule.launchActivity(new Intent());
        // loadingIdlingResource needs to be initialized here cause activity only exist till above line is called.
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        final String targerUrl = webServer.url(TEST_PATH).toString();

        // Click search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter target url
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(targerUrl), pressImeActionButton());

        // Check if target url is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(targerUrl)));

        // Open menu
        onView(allOf(withId(R.id.btn_menu), isDisplayed())).perform(click());

        // Open history panel
        onView(allOf(withId(R.id.menu_history), isDisplayed())).perform(click());

        // Click the first item in history panel
        onView(ViewMatchers.withId(R.id.browsing_history_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Check if this history record is target url and loaded again
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(targerUrl)));

    }

}