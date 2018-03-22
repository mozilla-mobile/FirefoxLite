/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Environment;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.StorageUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
public class DownloadTest {
    private static final String TEST_PATH = "/";
    public static final String IMAGE_FILE_NAME_DOWNLOADED = "download.jpg";
    public static final String IMAGE_FILE_NAME_RABBIT = "rabbit.jpg";

    public static final String HTML_ELEMENT_ID_DOWNLOAD = "download";
    private static final String TAG = "DownloadTest";

    private MockWebServer webServer;

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() throws Exception {
        clearDownloadFolder();
        activityRule.getActivity().finishAndRemoveTask();
    }


    private void clearDownloadFolder() {
        final File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (externalStoragePublicDirectory == null) {
            return;
        }

        for (File file : externalStoragePublicDirectory.listFiles()) {
            file.delete();
        }
    }

    @Rule
    public final GrantPermissionRule write_permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public final GrantPermissionRule read_permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<MainActivity>(MainActivity.class, true, false) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset("image_test.html"))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                // TODO: Below response are reserved for future download tests
                webServer.enqueue(new MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_RABBIT)));
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


    @Test
    public void triggerDownload_showToast() throws InterruptedException, UiObjectNotFoundException, IOException {

        // Start the activity
        activityRule.launchActivity(new Intent());
        final SessionLoadedIdlingResource sessionLoadedIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

        // Click and prepare to enter the URL
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        // Waiting for page loading completes
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);

        // Find the element in HTML with id "download" after the page is loaded
        onWebView()
                .withElement(findElement(Locator.ID, HTML_ELEMENT_ID_DOWNLOAD))
                .perform(webClick());

        // Unregister session loaded idling resource
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);

        // If there's no removable storage for Downloads, we skip this test
        try {
            final File dir = StorageUtils.getTargetDirOnRemovableStorageForDownloads(activityRule.getActivity(), "*/*");

            // Check if toast is displayed.
            onView(withText(R.string.download_started))
                    .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        } catch (NoRemovableStorageException e) {
            Log.e(TAG, "there's no removable storage for DownloadTest so we skip this.");
        }

        // TODO: 1. Long Click and check context menu 2. Check File name after downloads completed.
    }
}
