/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.CountChildViewMatcher;
import org.mozilla.focus.helper.GetNthChildViewMatcher;
import org.mozilla.focus.helper.GetTextViewMatcher;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.UnsupportedEncodingException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@Keep
@RunWith(AndroidJUnit4.class)
public class SearchSuggestionTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private Context context;

    @Before
    public void setUp() {
        // Load mock search engines
        SearchEngineManager.getInstance().loadSearchEngines(InstrumentationRegistry.getContext());
        AndroidTestUtils.beforeTest();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        activityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void clickSearchSuggestion_browseByDefaultSearchEngine() throws UiObjectNotFoundException, UnsupportedEncodingException {
        activityTestRule.launchActivity(new Intent());

        // Get the default search engine
        final SearchEngine defaultSearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(context);
        final SessionLoadedIdlingResource loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Click search field
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());

        // Type search text
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(typeText("zerda"));

        // Check if the suggestion count is shown at most 5
        onView(allOf(withId(R.id.search_suggestion), isDisplayed())).check(matches(CountChildViewMatcher.withChildViewCount(5, withId(R.id.suggestion_item))));

        // Pick a suggestion to click
        String text = GetTextViewMatcher.getText(GetNthChildViewMatcher.nthChildOf(withId(R.id.search_suggestion), 0));
        onView(allOf(withId(R.id.suggestion_item), withText(text), isDisplayed())).perform(click());

        // Wait for page is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);

        // Check if current url is matched with SearchEngine.buildSearchUrl()
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(defaultSearchEngine.buildSearchUrl((text)))));

        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

    }
}