/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.support.annotation.Keep
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.session
import org.mozilla.focus.utils.AndroidTestUtils

@Keep
@RunWith(AndroidJUnit4::class)
class TakeScreenshotTest {

    @JvmField
    @Rule
    val writePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    val readPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    @Before
    fun setUp() {
        AndroidTestUtils.beforeTest()
    }

    companion object {
        private val TARGET_URL_SITE = "file:///android_asset/gpl.html"
    }


    @Test
    fun takeScreenshot_screenshotIsCaptured() {

        session {

            loadPage(TARGET_URL_SITE)

        } takeScreenshot {

            clickMenuMyShots()

            clickFirstItemInMyShotsAndOpen()

            longClickAndDeleteTheFirstItemInMyShots()
        }

    }

}