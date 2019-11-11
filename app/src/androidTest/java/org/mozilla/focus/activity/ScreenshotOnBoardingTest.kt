/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask

@RunWith(AndroidJUnit4::class)
class ScreenshotOnBoardingTest {

    @JvmField
    @Rule
    val filePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        BeforeTestTask.Builder()
                .enableSreenshotOnBoarding(true)
                .build()
                .execute()
        activityTestRule.launchActivity(Intent())
    }

    companion object {
        private val TARGET_URL_SITE = "file:///android_asset/gpl.html"
    }

    /**
     * Test case no: TC0131
     * Test case name: Display screenshot on-boarding when back from recent app
     * Steps:
     * 1. Launch app with website loaded
     * 2. Tap screenshot capture button
     * 3. Tap recent app
     * 4. Tap Lite
     * 4. Check screenshot onboarding displayed
     * */
    @Test
    fun showScreenshotOnboarding_whenBackFromRecentApp() {
        val appName = context.getResources().getString(R.string.app_name)

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            firstTimeClickCaptureScreen()
            device.pressRecentApps()
            val liteApp = device.findObject(UiSelector().descriptionContains(appName))
            liteApp.click()
            checkScreenshotOnBoarding()
        }
    }

    /**
     * Test case no: TC0132
     * Test case name: Display screenshot on-boarding when back from home
     * Steps:
     * 1. Launch app with website loaded
     * 2. Tap screenshot capture button
     * 3. Tap home button
     * 4. Launch Lite
     * 5. Check no screenshot onboarding displayed
     * */
    @Test
    fun showScreenshotOnboarding_whenBackFromHome() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            firstTimeClickCaptureScreen()
            goHome(activityTestRule.activity)
            bringToForeground(activityTestRule.activity)
            checkScreenshotOnBoarding()
        }
    }

    /**
     * Test case no: TC0133
     * Test case name: Close screenshot on-boarding when screenshot repeatedly
     * Steps:
     * 1. Launch app with website loaded
     * 2. Tap screenshot capture button
     * 3. Check screenshot onboarding displayed
     * 4. Press back
     * 5. Tap screenshot capture button
     * 6. Check toast msg displayed but no screenshot on-boarding
     * */
    @Test
    fun closeScreenshotOnboarding_WhenScreenshotRepeatedly() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            firstTimeClickCaptureScreen()
            pressBack()
            clickCaptureScreen(activityTestRule.activity)
        }
    }

    /**
     * Test case no: TC0134
     * Test case name: Close Screenshot onboarding when open myshots from menu
     * Steps:
     * 1. Launch app with website loaded
     * 2. Tap screenshot capture button
     * 4. Press back
     * 5. Tap menu -> myshots
     * 6. Press back
     * 6. Check no screenshot on-boarding
     * 7. Tap menu
     * 8. Check no screenshot on-boarding
     * */
    @Test
    fun closeScreenshotOnboarding_WhenOpenMyShotsFromMenu() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            firstTimeClickCaptureScreen()
            pressBack()
            clickBrowserMenu()
            clickMenuMyShots()
            pressBack()
            checkNoScreenshotOnBoarding()
            clickBrowserMenu()
            checkNoScreenshotOnBoarding()
        }
    }

    /**
     * Test case no: TC0138
     * Test case name: Close Screenshot onboarding when open myshots from history
     * Steps:
     * 1. Launch app with website loaded
     * 2. Tap screenshot capture button
     * 3. Tap menu -> history -> myshots
     * 4. press back
     * 5. Tap menu
     * 4. Check no screenshot on-boarding
     * */
    @Test
    fun closeScreenshotOnboarding_WhenOpenMyshotsFromHistory() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            firstTimeClickCaptureScreen()
            pressBack()
            clickBrowserMenu()
            clickMenuHistory()
            clickPanelMyShots()
            pressBack()
            clickBrowserMenu()
            checkNoScreenshotOnBoarding()
        }
    }
}