/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
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
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;

@Keep
@RunWith(AndroidJUnit4.class)
public class BrowsingHistoryTest {

    private static final String PATH_SITE_1 = "/site1/";
    private static final String PATH_SITE_2 = "/site2/";
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
        new BeforeTestTask.Builder()
                .clearBrowsingHistory()
                .build()
                .execute();
        activityTestRule.launchActivity(new Intent());
        // loadingIdlingResource needs to be initialized here cause activity only exist till above line is called.
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());
    }

    @After
    public void tearDown() {
        // We unregister loadingIdlingResource here so other tests will not be affected.
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
    }

    /**
     * Test case no: TC0050
     * Test case name: History is recorded correctly
     * Steps:
     * 1. Launch app
     * 2. Visit some websites
     * 3. Tap Menu
     * 4. Tap History
     * 5. History is shown in correct order
     */
    @Test
    public void browsingTwoWebSites_sitesAreDisplayedInOrderInHistoryPanel() {

        final String targetUrlSite1 = webServer.url(PATH_SITE_1).toString();
        final String targetUrlSite2 = webServer.url(PATH_SITE_2).toString();

        // Click search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter target url
        onView(withId(R.id.url_edit)).perform(replaceText(targetUrlSite1), pressImeActionButton());

        // Check if site 1 is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(withText(targetUrlSite1)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        onView(withId(R.id.display_url)).perform(click());
        onView(withId(R.id.url_edit)).perform(replaceText(targetUrlSite2), pressImeActionButton());

        // Check if site 2 is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(withText(targetUrlSite2)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Open menu
        onView(allOf(withId(R.id.btn_menu), isDisplayed())).perform(click());

        // Open history panel
        onView(allOf(withId(R.id.menu_history), isDisplayed())).perform(click());

        onView(withId(R.id.browsing_history_recycler_view))
                .check(matches(atPosition(1, hasDescendant(withText(targetUrlSite2)))));

        onView(withId(R.id.browsing_history_recycler_view))
                .check(matches(atPosition(2, hasDescendant(withText(targetUrlSite1)))));

        // Click the first item in history panel, index 0 is date label so index 2 is the latest one of history item
        onView(ViewMatchers.withId(R.id.browsing_history_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(2, click()));

        // Check if this history record is target url and loaded again
        onView(withId(R.id.display_url)).check(matches(withText(targetUrlSite1)));

    }

    /**
     * Test case no: TC0051
     * Test case name: Remove a history record
     * Steps:
     * 1. Follow steps in his_001
     * 2. Tap on settings of a history record
     * 3. Context menu is shown with "delete" option
     * 4. Tap on delete
     * 5. History is deleted
     */
    @Test
    public void browsingWebsiteAndDeleteItsHistoryItem_deleteSuccessfully() {

        final String targetUrl = browsingWebsiteAndOpenHistoryPanel();

        // Click the first history item
        onView(withId(R.id.browsing_history_recycler_view))
                .check(matches(atPosition(1, hasDescendant(withText(targetUrl)))));

        // Open target history item's action menu
        onView(withId(R.id.browsing_history_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, clickChildViewWithId(R.id.history_item_btn_more)));

        // Click the delete button
        onView(withText(R.string.browsing_history_menu_delete))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Check if browsing history is cleared
        onView(withText(R.string.browsing_history_empty_view_msg)).check(matches(isDisplayed()));

    }


    /**
     * Test case no: TC0052
     * Test case name: Clear all history record
     * Steps:
     * 1. Follow steps in his_001
     * 2. Tap "CLEAR BROWSING HISTORY"
     * 3. Dialog shows up
     * 4. Tap "CANCEL"
     * 5. History is not changed
     * 6. Tap "CLEAR BROWSING HISTORY"
     * 7. Dialog shows up again
     * 8. Tap "CLEAR"
     * 9. History cleared
     */
    @Test
    public void clearBrowsingHistory_cancelAndClearWorkCorrectly() {

        browsingWebsiteAndOpenHistoryPanel();

        // Click the "Clear browsing history" button
        onView(withId(R.id.browsing_history_btn_clear)).perform(click());

        // Check if the dialog is showed
        onView(withText(R.string.browsing_history_dialog_confirm_clear_message)).check(matches(isDisplayed()));

        // Click cancel button
        onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click());

        // Click the "Clear browsing history" button
        onView(withId(R.id.browsing_history_btn_clear)).inRoot(isDialog()).perform(click());

        // Check if the dialog is showed
        onView(withText(R.string.browsing_history_dialog_confirm_clear_message)).check(matches(isDisplayed()));

        // Click clear button
        onView(withText(R.string.browsing_history_dialog_btn_clear)).inRoot(isDialog()).perform(click());

        // Check if browsing history is cleared
        onView(withText(R.string.browsing_history_empty_view_msg)).check(matches(isDisplayed()));

    }

    private String browsingWebsiteAndOpenHistoryPanel() {
        final String targetUrl = webServer.url(PATH_SITE_1).toString();

        // Click search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter target url
        onView(withId(R.id.url_edit)).perform(replaceText(targetUrl), pressImeActionButton());

        // Check if target url is loaded
        IdlingRegistry.getInstance().register(loadingIdlingResource);
        onView(withId(R.id.display_url)).check(matches(withText(targetUrl)));
        IdlingRegistry.getInstance().unregister(loadingIdlingResource);

        // Open menu
        onView(withId(R.id.btn_menu)).perform(click());

        // Open history panel
        onView(withId(R.id.menu_history)).perform(click());

        return targetUrl;
    }
}