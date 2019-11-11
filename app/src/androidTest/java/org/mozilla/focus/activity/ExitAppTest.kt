/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import androidx.annotation.Keep
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.session
import org.mozilla.focus.utils.AndroidTestUtils

@Keep
@RunWith(AndroidJUnit4::class)
class ExitAppTest {

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
     * Test case no: TC0046
     * Test case name: Exit app when view any website
     * Steps:
     * 1. Launch app
     * 2. Visit any website
     * 3. Tap on Menu and then Exit
     * 4. Check activity finishing or destroyed
     * */
    @Test
    fun exitAppWhenViewWebsite() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickBrowserMenu()
            clickExitApp()
        }

        // Check activity finishing or destroyed
        assertTrue(activityTestRule.activity.isFinishing() || activityTestRule.activity.isDestroyed())
    }

    /**
     * Test case no: TC0045
     * Test case name: Exit app when in home page
     * Steps:
     * 1. Launch app
     * 2. Tap settings -> exit
     * 3. Check activity finishing or destroyed
     */
    @Test
    fun exitAppWhenInHomePage() {

        session {
            clickHomeMenu()
            clickExitApp()
        }
        // Check activity finishing or destroyed
        assertTrue(activityTestRule.activity.isFinishing() || activityTestRule.activity.isDestroyed())
    }
}
