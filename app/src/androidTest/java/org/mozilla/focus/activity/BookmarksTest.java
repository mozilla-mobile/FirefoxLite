package org.mozilla.focus.activity;

import android.content.Intent;
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
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.CustomViewMatcher;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
public class BookmarksTest {

    private static final String TEST_SITE_1 = "/site1/";
    private static final String HTML_FILE_GET_LOCATION = "get_location.html";

    private MockWebServer webServer;

    private SessionLoadedIdlingResource sessionLoadedIdlingResource;

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
                .build()
                .execute();
        activityTestRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() {
        if  (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
    }

    /**
     * Test case no: 0203001
     * Test case name: Empty bookmarks list
     * Steps:
     * 1. Launch app
     * 2. Tap Menu
     * 3. Tap Bookmark button
     * 4. Show "No Bookmarks" in bookmarks panel */
    @Test
    public void openBookmarksPanel_showEmptyBookmarksView() {
        // Tap menu
        AndroidTestUtils.tapHomeMenuButton();
        // Tap Bookmark button
        onView(withId(R.id.menu_bookmark)).perform(click());
        // Show "No Bookmarks" in bookmarks panel
        onView(withId(R.id.empty_view_container)).check(matches(isDisplayed()));
    }

    /**
     * Test case no: 0203002, 0203003
     * Test case name: Add a website to bookmarks and remove it from bookmarks
     * Steps:
     * 1. Launch app
     * 2. Visit a website A
     * 3. Tap browser menu button
     * 4. Tap bookmark button
     * 5. Show bookmark saved snackbar
     * 6. Tap browser menu button
     * 7. Tap bookmark list button
     * 8. Tap the first item in bookmark list
     * 9. Show website A
     * 10. Tap browser menu button
     * 11. Bookmark button is activated
     * 12. Tap bookmark button
     * 13. Show bookmark removed toast */
    @Test
    public void addAndRemoveBookmarks_bookmarkIsAddedAndRemoved() {

        final String targetUrl = webServer.url(TEST_SITE_1).toString();
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        // Visit a website A
        AndroidTestUtils.tapHomeSearchField();
        AndroidTestUtils.typeTextInSearchFieldAndGo(targetUrl);

        // Check if target url is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);
        AndroidTestUtils.urlBarContainsText(targetUrl);
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        // Tap browser menu button
        AndroidTestUtils.tapBrowserMenuButton();

        // Tap bookmark button
        onView(withId(R.id.action_bookmark)).check(matches(not(CustomViewMatcher.isActivate()))).perform(click());

        // Show bookmark saved snackbar
        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText(R.string.bookmark_saved)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(android.support.design.R.id.snackbar_action), withText(R.string.bookmark_saved_edit)))
                .check(matches(isDisplayed()));

        // Tap browser menu button
        AndroidTestUtils.showHomeMenu(activityTestRule);

        // Tap bookmark list button
        onView(withId(R.id.menu_bookmark)).perform(click());

        // Tap the first item in bookmark list
        onView(ViewMatchers.withId(R.id.recyclerview))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Show website A
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);
        AndroidTestUtils.urlBarContainsText(targetUrl);
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        // Tap browser menu button
        AndroidTestUtils.showHomeMenu(activityTestRule);

        // Bookmark button is activated, and tap it
        onView(withId(R.id.action_bookmark)).check(matches(CustomViewMatcher.isActivate())).perform(click());

        // Show bookmark removed toast
        onView(withText(R.string.bookmark_removed))
                .inRoot(withDecorView(not(activityTestRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));
    }
}