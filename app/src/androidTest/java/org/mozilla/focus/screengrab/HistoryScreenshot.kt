/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screengrab

import android.content.Intent
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.annotation.ScreengrabOnly
import org.mozilla.focus.autobot.history
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.utils.AndroidTestUtils
import tools.fastlane.screengrab.FalconScreenshotStrategy
import tools.fastlane.screengrab.Screengrab
import java.io.IOException

@ScreengrabOnly
@RunWith(AndroidJUnit4::class)
class HistoryScreenshot : BaseScreenshot() {

    private lateinit var webServer: MockWebServer

    @JvmField
    @Rule
    var activityTestRule: ActivityTestRule<MainActivity> = object : ActivityTestRule<MainActivity>(MainActivity::class.java, true, false) {
        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()

            webServer = MockWebServer()
            try {
                webServer.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_GET_LOCATION))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"))
                webServer.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset(HTML_FILE_GET_LOCATION))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"))
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
        activityTestRule.launchActivity(Intent())
        Screengrab.setDefaultScreenshotStrategy(FalconScreenshotStrategy(activityTestRule.activity))
    }

    @Test
    fun screenshotHistory() {

        session {
            // browsing two web site, create two history record
            loadPageFromHomeSearchField(activityTestRule.activity, webServer.url(TEST_PATH_1).toString())
            loadLocalPageFromUrlBar(activityTestRule.activity, webServer.url(TEST_PATH_2).toString())
            clickBrowserMenu()
            clickMenuHistory()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.HISTORY_PANEL)
        }

        history {
            clickListItemActionMenu(1)
            checkItemMenuDeleteIsDisplayed()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.HISTORY_DELETE)
            clickItemMenuDelete()
            clickClearBrowsingHistory()
            checkConfirmClearDialogIsDisplayed()
            takeScreenshotViaFastlane(ScreenshotNamingUtils.HISTORY_CLEAR_ALL)
        }
    }

    companion object {

        private val TEST_PATH_1 = "/site1/"
        private val TEST_PATH_2 = "/site2/"
        private val HTML_FILE_GET_LOCATION = "get_location.html"
    }
}