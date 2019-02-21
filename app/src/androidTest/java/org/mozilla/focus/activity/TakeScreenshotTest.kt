/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.support.annotation.Keep
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.autobot.screenshot
import org.mozilla.focus.autobot.session
import org.mozilla.focus.screenshot.ScreenshotCaptureTask.saveBitmapToStorage
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.screenshot.model.Screenshot
import org.mozilla.focus.utils.AndroidTestUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Keep
@RunWith(AndroidJUnit4::class)
class TakeScreenshotTest {

    @JvmField
    @Rule
    val filePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @JvmField
    @Rule
    val activityTestRule = IntentsTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        AndroidTestUtils.beforeTest()
        activityTestRule.launchActivity(Intent())

        val context = activityTestRule.activity
        val title = "TeST"
        val url = "https://google.com"
        val content = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())

        val path = saveBitmapToStorage(
            context, "Screenshot_" + sdf.format(Date(timestamp)), content
        )

        FileUtils.notifyMediaScanner(activityTestRule.activity, path)

        val screenshot = Screenshot(title, url, timestamp, path)
        ScreenshotManager.getInstance().insert(screenshot, null)

    }

    companion object {
        private val TARGET_URL_SITE = "file:///android_asset/gpl.html"
    }

    /**
     * Test case no: TC0058
     * Test case name: Take Screenshot
     * Steps:
     * 1. Launch app
     * 2. Tap screenshot capture button
     * 3. Tap toast message displays "Screenshot saved"
     * 4. Tap menu
     * 5. Tap my shots viewer
     * 6. Tap first screenshot
     * 7. Tap delete
     * 8. Tap confirm delete
     * 9. Check it goes back to my shots viewer
     * */
    @Test
    fun takeScreenshot_screenshotIsCaptured() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickCaptureScreen(activityTestRule.activity)
            clickBrowserMenu()
            clickMenuMyShots()
        }

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            longClickAndDeleteTheFirstItemInMyShots()
        }
    }

    @Test
    fun screenShare() {
        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickCaptureScreen(activityTestRule.activity)
            clickBrowserMenu()
            clickMenuMyShots()
        }

        screenshot {
            clickFirstItemInMyShotsAndOpen()

        }

        // TODO: be a part of robot
        onView(ViewMatchers.withId(R.id.screenshot_viewer_btn_share)).perform(click())

        Intents.intended(
            CoreMatchers.allOf<Intent>(
                IntentMatchers.hasAction(CoreMatchers.equalTo<String>(Intent.ACTION_CHOOSER))
            )
        )
    }

    @Test
    fun insertManually() {

        session {
            clickBrowserMenu()
            clickMenuMyShots()
        }

        screenshot {
            clickFirstItemInMyShotsAndOpen()
        }
        onView(ViewMatchers.withId(R.id.screenshot_viewer_btn_share)).perform(click())

        Intents.intended(
            CoreMatchers.allOf<Intent>(
                IntentMatchers.hasAction(CoreMatchers.equalTo<String>(Intent.ACTION_CHOOSER))
            )
        )
    }
}