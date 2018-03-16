/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
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
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;

@Keep
@RunWith(AndroidJUnit4.class)
public class WebGeolocationPermissionTest {

    private static final String TEST_PATH = "/";
    private static final String HTML_FILE_GET_LOCATION = "get_location.html";
    private static final String HTML_ELEMENT_ID_GET_BUTTON = "get_button";
    private static final String HTML_ELEMENT_ID_RESULT = "result";
    private static final String RESULT_CHECK_TEXT = "Latitude";

    private MockWebServer webServer;

    @Rule
    public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<MainActivity>(MainActivity.class, true, false) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            webServer = new MockWebServer();
            try {
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
    public void tearDown() throws Exception {
        activityRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void webPopupGeoPermissionRequest_clickAllowAndUpdateGeoData() {

        // Start the activity
        activityRule.launchActivity(new Intent());
        final SessionLoadedIdlingResource sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

        // Click and prepare to enter the URL
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        // Waiting for page loading completes
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);

        // Find the element in HTML with id "get_button" and click it. "get_button" will try to access user's current location
        onWebView().withElement(findElement(Locator.ID, HTML_ELEMENT_ID_GET_BUTTON)).perform(webClick());

        // Check the Geolocation permission dialog is popup and click allow button
        onView(withText(R.string.geolocation_dialog_allow)).check(matches(isDisplayed())).perform(click());

        // Once the permission is granted, web page will start updating Geolocation
        onWebView().withElement(findElement(Locator.ID, HTML_ELEMENT_ID_RESULT)).check(webMatches(getText(), containsString(RESULT_CHECK_TEXT)));

        // Unregister session loaded idling resource
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        AndroidTestUtils.removeNewAddedTab();
    }

}