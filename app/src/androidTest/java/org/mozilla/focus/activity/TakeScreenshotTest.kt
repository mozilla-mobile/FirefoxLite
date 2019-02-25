/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.screenshot
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask

@RunWith(AndroidJUnit4::class)
class TakeScreenshotTest {

    @JvmField
    @Rule
    val filePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        BeforeTestTask.Builder().build().execute()
        intentsTestRule.launchActivity(Intent())

        // Make pre-existing screenshot
        session {
            loadPageFromHomeSearchField(intentsTestRule.activity, TARGET_URL_SITE)
            clickCaptureScreen(intentsTestRule.activity)
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
    @Ignore
    fun takeScreenshot_deleteScreenShot() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickAndDeleteTheFirstItemInMyShots()
        }
    }
}