package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.RootMatchers;
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
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;


@RunWith(AndroidJUnit4.class)
public class BookmarksTest {

    private static final String TEST_SITE_1 = "/site1/";
    private static final String HTML_FILE_GET_LOCATION = "get_location.html";
    private static final String MOCK_BOOKMARK_CONTENT = "mock_bookmark_content";
    private static final String MOCK_BOOKMARK_CONTENT_LONG = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.";

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
     * Test case no: TC0098
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
     * Test case no: TC0099
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

        final String targetUrl = browsingPageAndBookmarkPage();

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
        onView(withId(R.id.recyclerview))
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

    /**
     * Test case no: TC0100
     * Test case name: Remove bookmark from bookmark list
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Open bookmark list and tap the action menu of bookmark item
     * 3. Tap remove button
     * 4. Show "No Bookmarks" in bookmarks panel */
    @Test
    public void removeBookmarkFromBookmarkList_bookmarkIsRemoved() {

        tapBookmarkItemActionMenu();

        // Click the remove button
        onView(withText(R.string.remove_from_list))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Check if bookmark list is empty
        onView(withId(R.id.empty_view_container)).check(matches(isDisplayed()));

    }

    /**
     * Test case no: TC0101
     * Test case name: Edit bookmark name and location field
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Open bookmark list and tap the action menu of bookmark item
     * 3. Tap edit button
     * 4. Type some text in the name and location field
     * 5. Tap save button
     * 6. Bookmark item is updated */
    @Test
    public void editBookmarkWithChangingContent_bookmarkIsUpdated() {
        tapBookmarkItemActionMenu();

        // Click the edit button
        onView(withText(R.string.edit_bookmark))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Type some text in name field
        onView(withId(R.id.bookmark_name)).perform(replaceText(MOCK_BOOKMARK_CONTENT));

        // Type some text in location field
        onView(withId(R.id.bookmark_location)).perform(replaceText(MOCK_BOOKMARK_CONTENT));

        // Click the save button
        onView(withText(R.string.bookmark_edit_save)).perform(click());

        // We don't check "Bookmark updated" toast here since the toast is sent by EditBookmarkActivity which finish itself immediately
        // after show the toast. It might cause some inconsistent result once we check the visibility of toast.
        // Check toast work on local and ci/push but failed on ci/pr.
        // Check if the target item bookmark is updated
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(MOCK_BOOKMARK_CONTENT))))));

    }

    /**
     * Test case no: TC0102
     * Test case name: Do not change bookmark name and location field
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Open bookmark list and tap the action menu of bookmark item
     * 3. Tap edit button
     * 4. Do not change name and location field
     * 5. Save button is disabled */
    @Test
    public void editBookmarkWithoutChangingContent_saveButtonIsDisabled() {
        tapBookmarkItemActionMenu();

        // Click the edit button
        onView(withText(R.string.edit_bookmark))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Without changing bookmark content, check if save button is disabled
        onView(withText(R.string.bookmark_edit_save)).check(matches(not(isEnabled())));

    }

    /**
     * Test case no: TC0103
     * Test case name: Edit bookmark content and clear location content
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Open bookmark list and tap the action menu of bookmark item
     * 3. Tap edit button
     * 4. Clear the location field
     * 5. Save button is disabled */
    @Test
    public void editBookmarkWithClearingLocationContent_saveButtonIsDisabled() {
        tapBookmarkItemActionMenu();

        // Click the edit button
        onView(withText(R.string.edit_bookmark))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Clear the content of location field
        onView(withId(R.id.bookmark_location_clear)).perform(click());

        // Without changing bookmark content, check if save button is disabled
        onView(withText(R.string.bookmark_edit_save)).check(matches(not(isEnabled())));

    }

    /**
     * Test case no: TC0104
     * Test case name: Bookmark a web page, user can edit it immediately
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Tap edit button
     * 3. Change the name and location content
     * 4. Tap save button
     * 5. Check the item in bookmark list if we update it successfully */
    @Test
    public void addBookmarkAndEdit_bookmarkIsUpdated() {

        browsingPageAndBookmarkPage();

        // Click the edit button on snackbar
        onView(allOf(withId(android.support.design.R.id.snackbar_action), withText(R.string.bookmark_saved_edit)))
                .perform(click());

        // Type some text in name field
        onView(withId(R.id.bookmark_name)).perform(replaceText(MOCK_BOOKMARK_CONTENT));

        // Type some text in location field
        onView(withId(R.id.bookmark_location)).perform(replaceText(MOCK_BOOKMARK_CONTENT));

        // Click the save button
        onView(withText(R.string.bookmark_edit_save)).perform(click());

        // Open menu
        AndroidTestUtils.tapBrowserMenuButton();

        // Tap bookmark list button
        onView(withId(R.id.menu_bookmark)).perform(click());

        // Check if the first item of bookmark list is the bookmark we just edited
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(MOCK_BOOKMARK_CONTENT))))));

    }

    /**
     * Test case no: TC0105
     * Test case name: Edit bookmark content and clear location content
     * Steps:
     * 1. Launch app and add a web page to bookmark list
     * 2. Go to bookmark list and edit it
     * 3. Change the name and location content with various words
     * 4. Tap save button
     * 5. Check the item in bookmark list if we update it successfully */
    @Test
    public void editBookmarkWithVariousWords_bookmarkIsUpdated() {

        tapBookmarkItemActionMenu();

        // Click the edit button
        onView(withText(R.string.edit_bookmark))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Type some text in name field
        onView(withId(R.id.bookmark_name)).perform(replaceText(MOCK_BOOKMARK_CONTENT_LONG));

        // Type some text in location field
        onView(withId(R.id.bookmark_location)).perform(replaceText(MOCK_BOOKMARK_CONTENT_LONG));

        // Click the save button
        onView(withText(R.string.bookmark_edit_save)).perform(click());

        // Check if the first item of bookmark list is the bookmark we just edited
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(MOCK_BOOKMARK_CONTENT_LONG))))));

    }

    private String browsingPageAndBookmarkPage() {
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

        return targetUrl;
    }

    private void tapBookmarkItemActionMenu() {
        final String targetUrl = browsingPageAndBookmarkPage();

        // Tap browser menu button
        AndroidTestUtils.showHomeMenu(activityTestRule);

        // Tap bookmark list button
        onView(withId(R.id.menu_bookmark)).perform(click());

        // Check if the first item of bookmark list is the web page we just visited
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(targetUrl))))));

        // Open target bookmark item's action menu
        onView(withId(R.id.recyclerview)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.history_item_btn_more)));
    }
}