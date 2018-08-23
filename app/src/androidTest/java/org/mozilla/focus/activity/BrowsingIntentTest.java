/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.test.InstrumentationRegistry;
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
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabsSessionProvider;

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
    private static final String TARGET_URL_SITE_1 = "https://developer.mozilla.org/en-US/";
    private static final String TARGET_URL_SITE_2 = "https://developer.mozilla.org/en-US/Firefox_for_Android";

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
    }

    @Test
    public void receivedBrowsingIntent_tabIsCreated() {
        activityTestRule.launchActivity(new Intent());
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        sendBrowsingIntent();

        // Check if browser fragment is launched
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Check if target url is resolved and site 1 is loaded
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));

        // Click search button
        onView(withId(R.id.btn_search)).perform(click());

        // Browsing site 2
        onView(withId(R.id.url_edit)).perform(replaceText(TARGET_URL_SITE_2), pressImeActionButton());
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_2)));

        // Currently BrowserFragment need tabSession completes it's job to know if we can go back.
        // Without an idling resource there is pressBack() will leave the app and make the test crash
        // TODO: Add idling resource, or extend SessionIdlingResource to not only wait for onTabFinished,
        // TODO: but also wait for TabSession to complete. See canGoBack() in BrowserFragment
        // Click back
//        Espresso.pressBack();
//
//        // Check if site 1 is loaded again
//        onView(withId(R.id.display_url)).check(matches(withText(TARGET_URL_SITE_1)));
//        IdlingRegistry.getInstance().unregister(loadingIdlingResource);
//

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

        SessionManager sessionManager = TabsSessionProvider.getOrThrow(activityTestRule.getActivity());
        final int tabCount = sessionManager.getTabsCount();


        // Receive browsing intent
        sendBrowsingIntent();

        // Check if browser fragment is launched
        onView(withId(R.id.display_url)).check(matches(isDisplayed()));

        // Check if target url is resolved and site 1 is loaded
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
                .check(matches(withText(TARGET_URL_SITE_1)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Check if tab count is increased
        sessionManager = TabsSessionProvider.getOrThrow(activityTestRule.getActivity());
        Assert.assertTrue(sessionManager.getTabsCount() == tabCount + 1);

    }

    private void sendBrowsingIntent() {
        // Simulate third party app sending browsing url intent to rocket
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(TARGET_URL_SITE_1));
        final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        intent.setPackage(targetContext.getPackageName());
        targetContext.startActivity(intent);
    }
}