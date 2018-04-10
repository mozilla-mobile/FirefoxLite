/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.DownloadCompleteIdlingResource;
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
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public class GrantStoragePermissionTest {

    private static final String TEST_PATH = "/";
    private static final String IMAGE_FILE_NAME_DOWNLOADED_PREFIX = "rabbit";
    private static final String IMAGE_FILE_NAME_DOWNLOADED = "rabbit.jpg";
    private static final String HTML_FILE_FULL_SCREEN_IMAGE = "fullscreen_image_test.html";

    private MockWebServer webServer;
    private Context context;
    private SessionLoadedIdlingResource sessionLoadedIdlingResource;
    private DownloadCompleteIdlingResource downloadCompleteIdlingResource;

    @Rule
    public final GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public final GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<MainActivity>(MainActivity.class, true, false) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_FULL_SCREEN_IMAGE))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_DOWNLOADED)));
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_DOWNLOADED)));

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
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidTestUtils.beforeTest();

    }

    @After
    public void tearDown() {
        if (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
        activityRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void downloadImageAndDelete_deleteSuccessfully() {

        // Start main activity
        activityRule.launchActivity(new Intent());
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

        // Click home search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        // Waiting for the page is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);

        // Since test web page has a fullscreen image, we simulate the long click event at the center of image
        // Long click on image with popup context menu
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final int displayWidth = displayMetrics.widthPixels;
        final int displayHeight = displayMetrics.heightPixels;
        onView(withId(R.id.main_content)).check(matches(isDisplayed())).perform(AndroidTestUtils.clickXY(displayWidth / 2, displayHeight / 2, Tap.LONG));

        // Initialize DownloadCompleteIdlingResource and register content observer
        downloadCompleteIdlingResource = new DownloadCompleteIdlingResource(activityRule.getActivity());
        downloadCompleteIdlingResource.registerDownloadCompleteObserver();

        // Click save image button
        onView(allOf(withText(R.string.contextmenu_image_save), isDisplayed())).perform(click());

        // Wait for download complete
        IdlingRegistry.getInstance().register(downloadCompleteIdlingResource);

        // Open menu
        // Since right now snackbar will overlap with menu bar and we don't want to wait until snackbar is dismissed,
        // we cannot call onView(withId(R.id.btn_menu) here so call showMenu in MainActivity instead.
        AndroidTestUtils.showHomeMenu(activityRule);

        // Open download panel
        onView(withId(R.id.menu_download)).check(matches(isDisplayed())).perform(click());

        IdlingRegistry.getInstance().unregister(downloadCompleteIdlingResource);

        // Click the first download item and check if the name is matched
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))));

        // Open target download item's action menu
        onView(withId(R.id.recyclerview)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.menu_action)));

        // Click the remove button
        onView(withText(context.getString(R.string.delete_file)))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Check if delete successfully message is displayed
        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))
                .check(matches(isDisplayed()));

    }

}
