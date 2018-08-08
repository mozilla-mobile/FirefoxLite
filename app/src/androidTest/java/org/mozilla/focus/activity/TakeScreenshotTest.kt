/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.support.annotation.Keep
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.session
import org.mozilla.focus.utils.AndroidTestUtils

@Keep
@Ignore
@RunWith(AndroidJUnit4::class)
class TakeScreenshotTest {

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