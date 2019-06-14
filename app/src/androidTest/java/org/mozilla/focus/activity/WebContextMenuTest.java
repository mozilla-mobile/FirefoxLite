/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.FlakyTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.DisplayMetrics;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.helper.DownloadCompleteIdlingResource;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;


@RunWith(AndroidJUnit4.class)
public class WebContextMenuTest {

    private static final String TEST_PATH = "/";
    private static final String IMAGE_FILE_NAME_DOWNLOADED_PREFIX = "rabbit";
    private static final String IMAGE_FILE_NAME_DOWNLOADED = "rabbit.jpg";
    private static final String HTML_FILE_FULL_SCREEN_IMAGE = "fullscreen_image_test.html";

    private MockWebServer webServer;
    private SessionLoadedIdlingResource sessionLoadedIdlingResource;
    private DownloadCompleteIdlingResource downloadCompleteIdlingResource;

    @Rule
    public final GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public final GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule = new IntentsTestRule<MainActivity>(MainActivity.class, true, false) {
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
        new BeforeTestTask.Builder()
                .build()
                .execute();
        intentsTestRule.launchActivity(new Intent());
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

    /**
     * Test case no: TC0081
     * Test case name: Empty bookmarks list
     * Steps:
     * 1. Launch Rocket and visit a website with links
     * 2. Long click to show pop context menu
     * 3. Check "Open link in new tab" and "switch" displayed
     * 4. Click switch
     * 5. Check tab count is 2
     */
    @Test
    public void openLinkInNewTab() {

        loadTestWebsiteAndOpenContextMenu();

        // Click "Open link in new tab"
        onView(withText(R.string.contextmenu_open_in_new_tab)).perform(click());

        // Check if "New tab opened" text is shown in snack bar
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), withText(R.string.new_background_tab_hint))).check(matches(isDisplayed()));

        // Check if switch is shown in snack bar
        onView(allOf(withId(R.id.snackbar_action), withText(R.string.new_background_tab_switch))).check(matches(isDisplayed()));

        // Click switch
        onView(allOf(withId(R.id.snackbar_action), withText(R.string.new_background_tab_switch))).check(matches(isDisplayed())).perform(click());

        // Check tab count is 2
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(new BottomBarRobot().browserBottomBarItemView(R.id.bottom_bar_tab_counter)))).check(matches(withText("2")));
    }

    /**
     * Test case no: TC0017
     * Test case name: Share link
     * Steps:
     * 1. Launch Rocket and visit a website with links
     * 2. Long press on a link
     * 3. Tap on the "Share link" on the prompt dialog.
     * 5. Check intent sent
     */
    @Test
    public void shareLink_sendShareIntent() {

        loadTestWebsiteAndOpenContextMenu();

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click "Share link"
        onView(allOf(withText(R.string.contextmenu_link_share), isDisplayed())).perform(click());

        // Check if intent is sent
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent.ACTION_SEND), hasExtra(Intent.EXTRA_TEXT, getLinkUrl())))));
    }

    /**
     * Test case no: TC0184
     * Test case name: Copy link address
     * Steps:
     * 1. Launch Rocket and visit a website with links
     * 2. Long press on a link
     * 3. Tap on "Copy link address"
     * 5. Check copied url matches as expected
     */
    @Test
    public void copyLinkAddress() {

        loadTestWebsiteAndOpenContextMenu();

        // Click "Copy link address"
        onView(allOf(withText(R.string.contextmenu_link_copy), isDisplayed())).perform(click());

        // Get clip data form clip board
        final ClipboardManager clipboard = (ClipboardManager)
                intentsTestRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clipData = clipboard.getPrimaryClip();
        Assert.assertNotNull(clipData);

        final ClipData.Item clipItem = clipData.getItemAt(0);
        Assert.assertNotNull(clipItem);

        // Check if uri is matched
        Assert.assertEquals(clipItem.getUri(), Uri.parse(getLinkUrl()));
    }

    /**
     * Test case no: TC0185
     * Test case name: Share image
     * Steps:
     * 1. Launch Rocket and visit a website with images
     * 2. Long press on the selected image
     * 3. Tap on the "Share image"
     * 4. Tap on the "Gmail"
     * 5. Check intent sent
     */
    @Test
    public void shareImage_sendShareIntent() {

        loadTestWebsiteAndOpenContextMenu();

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        // Click "Share image"
        onView(allOf(withText(R.string.contextmenu_image_share), isDisplayed())).perform(click());

        // Check if intent is sent
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent.ACTION_SEND), hasExtra(Intent.EXTRA_TEXT, getLinkUrl())))));
    }

    /**
     * Test case no: TC0186
     * Test case name: Copy image address
     * Steps:
     * 1. Launch Rocket and visit a website with images
     * 2. Long press on the selected image
     * 3. Tap on "Copy image address"
     * 5. Check copied url matches as expected
     */
    @Test
    public void copyImageAddress() {

        loadTestWebsiteAndOpenContextMenu();

        // Click "Copy image address"
        onView(allOf(withText(R.string.contextmenu_image_copy), isDisplayed())).perform(click());

        // Get clip data form clip board
        final ClipboardManager clipboard = (ClipboardManager)
                intentsTestRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clipData = clipboard.getPrimaryClip();
        Assert.assertNotNull(clipData);

        final ClipData.Item clipItem = clipData.getItemAt(0);
        Assert.assertNotNull(clipItem);

        // Check if uri is matched
        Assert.assertEquals(clipItem.getUri(), Uri.parse(getLinkUrl()));
    }

    /**
     * Test case no: TC0187
     * Test case name: Save image
     * Steps:
     * 1. Launch Rocket and visit a website with images
     * 2. Long press on the selected image
     * 3. Tap on "Save image"
     * 5. Click menu
     * 6. Click download manager
     * 7. Check download item matches as expected
     */
    @FlakyTest
    @Test
    public void saveImageThenDelete_imageSaveAndDeleteSuccessfully() {

        loadTestWebsiteAndOpenContextMenu();

        // Initialize DownloadCompleteIdlingResource and register content observer
        downloadCompleteIdlingResource = new DownloadCompleteIdlingResource(intentsTestRule.getActivity());
        downloadCompleteIdlingResource.registerDownloadCompleteObserver();

        // Click save image button
        onView(allOf(withText(R.string.contextmenu_image_save), isDisplayed())).perform(click());

        // Wait for download complete
        IdlingRegistry.getInstance().register(downloadCompleteIdlingResource);

        // Open menu
        // Since right now snackbar will overlap with menu bar and we don't want to wait until snackbar is dismissed,
        // we cannot call onView(withId(R.id.btn_menu) here so call showMenu in MainActivity instead.
        AndroidTestUtils.showMenu(intentsTestRule);

        // Open download panel
        onView(withId(R.id.menu_download)).check(matches(isDisplayed())).perform(click());

        IdlingRegistry.getInstance().unregister(downloadCompleteIdlingResource);

        // Check if first download item name is matched
        onView(withId(R.id.recyclerview))
                .check(matches(atPosition(0, hasDescendant(withText(containsString(IMAGE_FILE_NAME_DOWNLOADED_PREFIX))))));
    }

    private void loadTestWebsiteAndOpenContextMenu() {
        sessionLoadedIdlingResource = new SessionLoadedIdlingResource(intentsTestRule.getActivity());
        // Click home search field
        onView(withId(R.id.home_fragment_fake_input)).perform(click());

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        // Waiting for the page is loaded
        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource);

        // Since test web page has a fullscreen image, we simulate the long click event by clicking at the center of image
        // Long click on image with popup context menu
        final DisplayMetrics displayMetrics = intentsTestRule.getActivity().getResources().getDisplayMetrics();
        final int displayWidth = displayMetrics.widthPixels;
        final int displayHeight = displayMetrics.heightPixels;
        onView(withId(R.id.main_content)).check(matches(isDisplayed())).perform(AndroidTestUtils.clickXY(displayWidth / 2, displayHeight / 2, Tap.LONG));
        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource);
    }

    private String getLinkUrl() {
        return webServer.url(TEST_PATH).toString() + IMAGE_FILE_NAME_DOWNLOADED;
    }

}
