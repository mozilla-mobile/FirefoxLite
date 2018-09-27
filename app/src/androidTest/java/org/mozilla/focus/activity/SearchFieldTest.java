/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import androidx.annotation.Keep;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@Keep
@RunWith(AndroidJUnit4.class)
public class SearchFieldTest {

    private static final String TYPED_GENERAL_TEXT = "zerda";
    private static final String TYPED_GREEK_TEXT  = "αβπΣ";
    private static final String TYPED_SCIENCE_SYMBOLS_TEXT  = "√°C °F";

    private SessionLoadedIdlingResource loadingIdlingResource;

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Load mock search engines
        SearchEngineManager.getInstance().loadSearchEngines(InstrumentationRegistry.getContext());
        AndroidTestUtils.beforeTest();
        activityTestRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() throws Exception {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
    }

    @Test
    public void typeTextInSearchFieldAndClear_textIsClearedAndBackToHome() {

        // Click home search field
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());

        // Check clear button is not displayed when there's no text
        onView(withId(R.id.clear)).check(matches(not(isDisplayed())));

        // Type some text
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TYPED_GENERAL_TEXT));

        // Click clear button
        onView(allOf(withId(R.id.clear), isDisplayed())).perform(click());

        // Check if the text is cleared
        onView(allOf(withId(R.id.url_edit), isDisplayed())).check(matches(withText("")));

        // Type some text and press back twice
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TYPED_GENERAL_TEXT), closeSoftKeyboard());

        // Back to home
        Espresso.pressBack();

        // Check if we are back to home and click home search field again
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());

        // Type some text
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TYPED_GENERAL_TEXT));

        // Click outside the search field and soft keyboard
        onView(allOf(withId(R.id.dismiss), isDisplayed())).perform(click());

        // Check if we are back to home
        onView(withId(R.id.home_fragment_fake_input)).check(matches(isDisplayed()));

    }

    @Test
    public void typeSpecialCharactersInSearchField_searchIsPerformingAccordingly() {

        // Get the default search engine
        final SearchEngine defaultSearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(InstrumentationRegistry.getInstrumentation().getTargetContext());
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Click home search field
        onView(allOf(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click());

        // Type some greek characters
        onView(allOf(withId(R.id.url_edit), isDisplayed())).perform(replaceText(TYPED_GREEK_TEXT), pressImeActionButton());

        // Wait for the page is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);

        // Check if current url is matched with SearchEngine.buildSearchUrl()
        onView(AllOf.allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(defaultSearchEngine.buildSearchUrl((TYPED_GREEK_TEXT)))));

        // Since we will click search button later and UrlInputFragment will be displayed so we need to unregister IdlingResource here
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Click search button
        onView(withId(R.id.btn_search)).perform(click());

        // Type some science characters
        onView(withId(R.id.url_edit)).perform(clearText()).perform(replaceText(TYPED_SCIENCE_SYMBOLS_TEXT), pressImeActionButton());

        // Wait for the page is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);

        // Check if current url is matched with SearchEngine.buildSearchUrl()
        onView(AllOf.allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(defaultSearchEngine.buildSearchUrl((TYPED_SCIENCE_SYMBOLS_TEXT)))));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

    }
}