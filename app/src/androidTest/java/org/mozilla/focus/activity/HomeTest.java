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

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.TopSitesUtils;

import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class HomeTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);
    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() throws Exception {
        // Remove activity from Recent Apps
        activityRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void clickTopSite_loadTopSite() {

        // Now start the activity
        activityRule.launchActivity(new Intent());
        loadingIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

        final MainActivity context = activityRule.getActivity();

        try {
            // Get test top sites
            final JSONArray jsonDefault = new JSONArray(Inject.getDefaultTopSites(context));
            final List<Site> defaultSites = TopSitesUtils.paresJsonToList(context, jsonDefault);

            // Check the title of the sample top site is correct
            onView(withId(R.id.main_list))
                    .check(matches(atPosition(0, hasDescendant(withText(defaultSites.get(0).getTitle())))));

            // Click and load the sample top site
            onView(ViewMatchers.withId(R.id.main_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

            // After page loading completes
            IdlingRegistry.getInstance().register(loadingIdlingResource);

            // Check if the url is displayed correctly
            onView(withId(R.id.display_url))
                    .check(matches(allOf(withText(defaultSites.get(0).getUrl()), isDisplayed())));

            // Always remember to unregister idling resource
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        } catch (JSONException e) {
            e.printStackTrace();
            throw new AssertionError("testTopSite failed:", e);

        }
    }

}