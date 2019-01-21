/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import android.support.annotation.Keep
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
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
    val activityTestRule = IntentsTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        AndroidTestUtils.beforeTest()
        activityTestRule.launchActivity(Intent())

        // Make pre-existing screenshot
        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickCaptureScreen(activityTestRule.activity)
            clickBrowserMenu()
            clickMenuMyShots()
        }
    }

    companion object {
        private val TARGET_URL_SITE = "file:///android_asset/gpl.html"
    }

    /**
     * Test case no: TC0058
     * Test case name: Take Screenshot and delete
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Tap delete
     * 4. Tap confirm delete
     * 5. Check it goes back to my shots viewer
     * */
    @Test
    fun takeScreenshot_deleteScreenShot() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickAndDeleteTheFirstItemInMyShots()
        }
    }

    /**
     * Test case no: TC0057
     * Test case name: Take Screenshot and view info
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click info
     * 4. Check image dialog is displayed
     * 5. press back
     * 6. Check image info dialog is dismissed
     * */
    @Test
    fun takeScreenshot_viewInfo() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickInfoTheFirstItemInMyShots()
            pressBack()
            checkScreenshotInfoDialogDimissed()
        }
    }

    /**
     * Test case no: TC0054
     * Test case name: Take Screenshot and open the web
     * Steps:
     * 1. Given preexisting screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click open the web
     * 5. Check url matches
     * */
    @Test
    fun takeScreenshot_openWeb() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickOpenWebInScreenshotViewer(TARGET_URL_SITE)
        }
    }

    /**
     * Test case no: TC0054
     * Test case name: Take Screenshot and share
     * Steps:
     * 1. Given preexisting screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click share
     * 5. Check intent is sent
     * */
    @Test
    fun takeScreenshot_share() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickShareInScreenshotViewer(TARGET_URL_SITE)
        }
    }

    /**
     * Test case no: TC0055
     * Test case name: Take Screenshot and edit
     * Steps:
     * 1. Given preexisting screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click edit
     * 5. Check intent is sent
     * */
    @Test
    fun takeScreenshot_Edit() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickEditInScreenshotViewer()
        }
    }
}