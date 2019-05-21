/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab

import android.Manifest
import android.content.Intent
import android.os.Build
import android.support.test.filters.SdkSuppress
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.annotation.ScreengrabOnly
import org.mozilla.focus.autobot.bottomBar
import org.mozilla.focus.autobot.indexOfType
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.utils.AndroidTestUtils
import org.mozilla.rocket.chrome.BottomBarItemAdapter.Companion.TYPE_TAB_COUNTER
import org.mozilla.rocket.chrome.BottomBarViewModel.Companion.DEFAULT_BOTTOM_BAR_ITEMS
import tools.fastlane.screengrab.FalconScreenshotStrategy
import tools.fastlane.screengrab.Screengrab
import java.io.IOException

@ScreengrabOnly
@RunWith(AndroidJUnit4::class)
class BrowserFragmentScreenshot : BaseScreenshot() {

    private lateinit var webServer: MockWebServer

    @JvmField
    @Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    var activityTestRule: ActivityTestRule<MainActivity> = object : ActivityTestRule<MainActivity>(MainActivity::class.java, true, false) {
        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()

            webServer = MockWebServer()

            try {
                webServer.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_FULL_SCREEN_IMAGE))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"))
                webServer.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_DOWNLOADED)))
                webServer.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(IMAGE_FILE_NAME_DOWNLOADED)))

                webServer.start()
            } catch (e: IOException) {
                throw AssertionError("Could not start web server", e)
            }
        }

        override fun afterActivityFinished() {
            super.afterActivityFinished()
            try {
                webServer.close()
                webServer.shutdown()
            } catch (e: IOException) {
                throw AssertionError("Could not stop web server", e)
            }
        }
    }

    @Before
    fun setUp() {
        BeforeTestTask.Builder().build().execute()
        // Start main activity
        activityTestRule.launchActivity(Intent())
        Screengrab.setDefaultScreenshotStrategy(FalconScreenshotStrategy(activityTestRule.activity))
    }

    @Test
    fun screenshotBrowserFragment() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, webServer.url(TEST_PATH).toString())
            longClickOnWebViewContent(activityTestRule.activity)
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_CONTEXT_MENU)
            clickOpenLinkInNewTab()
            checkNewTabOpenedSnackbarIsDisplayed()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_NEW_TAB_OPENED)
            loadErrorPage(TARGET_URL_ERROR_PAGE)
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_ERROR_PAGE)

            // Simulate show no storage permission snackbar
            MockUIUtils.showSnackbarAndWait(activityTestRule.activity, R.string.permission_toast_location, R.string.permission_handler_permission_dialog_setting)
            checkNoLocationPermissionSnackbarIsDisplayed()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_NO_LOCATION_SNACKBAR)

            // Simulate show no geo permission dialog
            MockUIUtils.showGeoPromptDialog(activityTestRule.activity, TARGET_URL_ERROR_PAGE)
            checkGeoPermissionDialogIsDisplayed()

            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_NO_LOCATION_DIALOG)
            clickAllowGeoPermission()
        }
    }

    @Test
    fun screenshotTabTray() {
        session {
            loadPageFromHomeSearchField(activityTestRule.activity, webServer.url(TEST_PATH).toString())
            bottomBar {
                clickBrowserBottomBarItem(DEFAULT_BOTTOM_BAR_ITEMS.indexOfType(TYPE_TAB_COUNTER))
            }
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_TAB_TRAY)
            clickCloseAllTabs()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_TAB_TRAY_CLOSE_DIALOG)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun screenshotBrowserFragmentTextAction() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_LICENSE)
            longClickOnWebViewContent(activityTestRule.activity)
            clickTextActionMore()
//            checkSearchInRocketIsDisplayed()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.BROWSER_TEXT_ACTION_DAILOG)
        }
    }

    companion object {

        private val TEST_PATH = "/"
        private val TARGET_URL_ERROR_PAGE = "http://a.b.c.d.e"
        private val IMAGE_FILE_NAME_DOWNLOADED = "rabbit.jpg"
        private val HTML_FILE_FULL_SCREEN_IMAGE = "fullscreen_image_test.html"

        private val TARGET_URL_LICENSE = "file:///android_asset/gpl.html"
    }
}
