/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.TopSitesUtils;

import java.util.List;
import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class RemoveTopSitesTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private List<Site> siteList;
    private Context context;
    private String removeLabel;

    @Before
    public void setUp() throws JSONException {
        AndroidTestUtils.beforeTest();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        removeLabel = context.getString(R.string.remove);
        prepareTopSiteList();
        activityTestRule.launchActivity(new Intent());
    }

    @Test
    public void deleteTopSite_deleteSuccessfully() {

        // Pick a test site to delete
        final int siteIndex = new Random().nextInt(siteList.size());
        final Site testSite = siteList.get(siteIndex);

        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));

        // Long click the test site
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(siteIndex, longClick()));

        // Check the remove button is displayed
        onView(withText(removeLabel)).check(matches(isDisplayed()));

        // Click the remove button
        onView(withText(removeLabel))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Check the test site is removed
        onView(withId(R.id.main_list))
                .check(matches(not(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle()))))));
    }

    @Test
    public void deleteTopSiteAndCancel_topSiteIsStillThere() {

        // Pick a test site to test
        final int siteIndex = new Random().nextInt(siteList.size());
        final Site testSite = siteList.get(siteIndex);

        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));

        // Long click the test site
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(siteIndex, longClick()));

        // Check the remove button is displayed
        onView(withText(removeLabel))
                .check(matches(isDisplayed()));

        // Press the back key
        Espresso.pressBack();

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));
    }

    private void prepareTopSiteList() throws JSONException {
        final JSONArray jsonArray = new JSONArray(Inject.getDefaultTopSites(context));
        siteList = TopSitesUtils.paresJsonToList(context, jsonArray);

        Assert.assertNotNull(siteList);
        Assert.assertTrue(siteList.size() > 0);

    }

}