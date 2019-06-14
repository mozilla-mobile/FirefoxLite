/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.annotation.ScreengrabOnly;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.DownloadCompleteIdlingResource;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId;

@ScreengrabOnly
@RunWith(AndroidJUnit4.class)
public class DownloadScreenshot extends BaseScreenshot {

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
        new BeforeTestTask.Builder().build().execute();
        // Start main activity
        activityRule.launchActivity(new Intent());
        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(activityRule.getActivity()));
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

    }

    @After
    public void tearDown() {
        if (sessionLoadedIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
        }
        if (downloadCompleteIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(downloadCompleteIdlingResource);
        }
    }

    @Test
    public void screenshotDownload() {

        // Browse a web page that has download content
        onView(withId(R.id.home_fragment_fake_input)).perform(click());
        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());
        // Waiting for the page is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);
        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(webServer.url(TEST_PATH).toString())));
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        // Simulate show no download permission snackbar
        MockUIUtils.showSnackbarAndWait(activityRule.getActivity(), R.string.permission_toast_storage, R.string.permission_handler_permission_dialog_setting);
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), withText(R.string.permission_toast_storage))).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_NO_PERMISSION);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        MockUIUtils.showToast(activityRule.getActivity(), R.string.download_started);
        AndroidTestUtils.toastContainsText(activityRule.getActivity(), R.string.download_started);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_DOWNLOADING);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        MockUIUtils.showToast(activityRule.getActivity(), R.string.download_file_not_supported);
        AndroidTestUtils.toastContainsText(activityRule.getActivity(), R.string.download_file_not_supported);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_PROTOCOL_NOT_SUPPORT);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        MockUIUtils.showToast(activityRule.getActivity(), R.string.message_removable_storage_space_not_enough);
        AndroidTestUtils.toastContainsText(activityRule.getActivity(), R.string.message_removable_storage_space_not_enough);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_SD_FULL);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

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
        // Web context menu takes time to disappear, we delay a while here
        SystemClock.sleep(MockUIUtils.POPUP_DELAY);

        // Wait for download complete
        IdlingRegistry.getInstance().register(downloadCompleteIdlingResource);
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))
                .check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_DOWNLOADED);

        SystemClock.sleep(MockUIUtils.LONG_DELAY);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_DOWNLOADED);

        // Open menu
        // Since right now snackbar will overlap with menu bar and we don't want to wait until snackbar is dismissed,
        // we cannot call onView(withId(R.id.btn_menu) here so call showMenu in MainActivity instead.
        AndroidTestUtils.showMenu(activityRule);

        // Open download panel
        onView(withId(R.id.menu_download)).check(matches(isDisplayed())).perform(click());

        IdlingRegistry.getInstance().unregister(downloadCompleteIdlingResource);

        // Click the first download item and check if the name is matched
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))));

        // Open target download item's action menu
        onView(withId(R.id.recyclerview)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.menu_action)));

        onView(withText(R.string.delete_file))
                .inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()));
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_LIST_MENU);
        // Click the remove button
        onView(withText(context.getString(R.string.delete_file)))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        SystemClock.sleep(MockUIUtils.POPUP_DELAY);
        // Check if delete successfully message is displayed
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))
                .check(matches(isDisplayed()));

        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_DELETED);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        MockUIUtils.showToast(activityRule.getActivity(), R.string.download_cancel);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_CANCELED);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

        MockUIUtils.showToast(activityRule.getActivity(), R.string.cannot_find_the_file);
        Screengrab.screenshot(ScreenshotNamingUtils.DOWNLOAD_CANT_FIND_THE_FILE);
        SystemClock.sleep(MockUIUtils.SHORT_DELAY);

    }

}
