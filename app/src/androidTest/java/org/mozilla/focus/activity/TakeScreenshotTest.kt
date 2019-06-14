/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import androidx.annotation.Keep
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.screenshot
import org.mozilla.focus.autobot.session
import org.mozilla.focus.utils.AndroidTestUtils

@Keep
@RunWith(AndroidJUnit4::class)
class TakeScreenshotTest {

    @JvmField
    @Rule
    val filePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        AndroidTestUtils.beforeTest()
        activityTestRule.launchActivity(Intent())
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
    @Ignore
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
}