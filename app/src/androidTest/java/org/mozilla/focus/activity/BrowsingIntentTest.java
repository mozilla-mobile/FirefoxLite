/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;
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
public class BrowsingIntentTest {

    private SessionLoadedIdlingResource loadingIdlingResource;
    private static final String TEST_PATH = "/";
    private static final String TARGET_URL_SITE_1 = "get_location.html";
    private static final String TARGET_URL_SITE_2 = "file:///android_asset/gpl.html";

    private MockWebServer webServer;

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
        webServer = new MockWebServer();
        try {
            webServer.enqueue(new MockResponse()
                    .setBody(AndroidTestUtils.readTestAsset(TARGET_URL_SITE_1))
                    .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
            webServer.start();
        } catch (IOException e) {
            throw new AssertionError("Could not start web server", e);
        }
    }

    @After
    public void tearDown() {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
        try {
            if (webServer != null) {
                webServer.close();
                webServer.shutdown();
            }
        } catch (IOException e) {
            throw new AssertionError("Could not stop web server", e);
        }
        if (activityTestRule.getActivity() != null) {
            activityTestRule.getActivity().finishAndRemoveTask();
        }
    }

    @Test
    public void receivedBrowsingIntent_tabIsCreated() {

        sendBrowsingIntent();

        // Check if browser fragment is launched
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Check if target url is resolved and site 1 is loaded
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(webServer.url(TEST_PATH).toString())));
        /** We need to unregister the SessionLoadedIdlingResource immediately once the loading is done. If not doing so, the next espresso
         *  action "Click search button" will fail to pass the check "isIdleNow()" in SessionLoadedIdlingResource since getVisibleBrowserFragment() is null now.
         *  See also in {@link org.mozilla.focus.helper.SessionLoadedIdlingResource#isIdleNow() isIdleNow}.
         */
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Click search button
        onView(withId(R.id.btn_search)).perform(click());

        // Browsing site 2
        onView(withId(R.id.url_edit)).perform(replaceText(TARGET_URL_SITE_2), pressImeActionButton());
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_2)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Click back
        Espresso.pressBack();

        // Check if site 1 is loaded again
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(webServer.url(TEST_PATH).toString())));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

    }

    @Test
    public void appHasOneTabAndReceiveBrowsingIntent_tabIncreasedAndBrowse() {

        // Launch activity
        activityTestRule.launchActivity(new Intent());

        // Click search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Browsing site 2
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TARGET_URL_SITE_2), pressImeActionButton());
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_2)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        TabsSession tabsSession = TabsSessionProvider.getOrThrow(activityTestRule.getActivity());
        final int tabCount = tabsSession.getTabsCount();

        // leave app
        activityTestRule.finishActivity();

        // Receive browsing intent
        sendBrowsingIntent();

        // Since we relaunched activity, so we need to pass a new activity reference to IdlingResource again
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Check if browser fragment is launched
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Check if target url is resolved and site 1 is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(webServer.url(TEST_PATH).toString())));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Check if tab count is increased
        tabsSession = TabsSessionProvider.getOrThrow(activityTestRule.getActivity());
        Assert.assertTrue( tabsSession.getTabsCount() == tabCount + 1);

    }

    private void sendBrowsingIntent() {
        // Simulate third party app sending browsing url intent to rocket
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(webServer.url(TEST_PATH).toString()));
        activityTestRule.launchActivity(intent);
    }
}