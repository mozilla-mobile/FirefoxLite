/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.mozilla.focus.utils.AndroidTestUtils.getResourceId;

@Keep
@RunWith(AndroidJUnit4.class)
public class SearchSuggestionTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    private Context context;
    private UiDevice uiDevice;

    @Before
    public void setUp() throws JSONException {
        AndroidTestUtils.beforeTest();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @After
    public void tearDown() {
        activityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void searchSuggestionTest() throws UiObjectNotFoundException, UnsupportedEncodingException {

        final SessionLoadedIdlingResource loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Click search field
        onView(withId(R.id.home_fragment_fake_input))
                .perform(click());

        onView(withId(R.id.url_edit)).check(matches(isDisplayed()));

        // Check if the soft keyboard is shown
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        Assert.assertTrue(inputMethodManager.isAcceptingText());

        onView(withId(R.id.url_edit)).perform(typeText("rocket"));

        onView(withId(R.id.search_suggestion)).check(matches(isDisplayed()));
        // Check if the suggestion count is shown at most 5
        final UiCollection suggestionCollection = new UiCollection(new UiSelector().resourceId(getResourceId("search_suggestion")));
        Assert.assertTrue(suggestionCollection.exists());
        final int suggestionCount = suggestionCollection.getChildCount(new UiSelector().resourceId(getResourceId("suggestion_item")));
        Assert.assertTrue(suggestionCount <= 5);

        // Pick a suggestion
        final UiObject suggestionItem = suggestionCollection.getChild(new UiSelector().index(new Random().nextInt(suggestionCount)));
        final String suggestionText = suggestionItem.getText();
        suggestionItem.click();

        // Get default search engine
        final SearchEngine defaultSearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(context);

        // After page loading completes
        IdlingRegistry.getInstance().register(loadingIdlingResource);

        final UiObject urlBar = uiDevice.findObject(new UiSelector().resourceId(getResourceId("display_url")));
        Assert.assertTrue(urlBar.exists());

        // Check if the search result is using default search engine
        final String[] searchEngine = defaultSearchEngine.getName().toLowerCase().split(" ");
        Assert.assertTrue(urlBar.getText().contains(searchEngine[0]));

        // Check if the search result is matched the suggestion we picked
        Assert.assertTrue(URLDecoder.decode(urlBar.getText(), "UTF-8").contains(suggestionText));

        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Remove new added tab
        AndroidTestUtils.removeNewAddedTab();

    }

}