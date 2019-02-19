/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.support.test.espresso.IdlingRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.autobot.notification
import org.mozilla.focus.autobot.private
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.helper.SessionLoadedIdlingResource
import org.mozilla.focus.screengrab.BaseScreenshot
import org.mozilla.rocket.privately.PrivateModeActivity

@RunWith(AndroidJUnit4::class)
class PrivateModeNotification : BaseScreenshot() {

    private var loadingIdlingResource: SessionLoadedIdlingResource? = null
    private var notificationManager: NotificationManager? = null

    @JvmField
    @Rule
    var activityTestRule = ActivityTestRule(PrivateModeActivity::class.java, true, false)

    @Before
    fun setUp() {
        BeforeTestTask.Builder().build().execute()

        // launch app
        activityTestRule.launchActivity(Intent())
        notificationManager = activityTestRule.activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notification {
            dismissNotification(activityTestRule.activity)
        }
    }

    @After
    fun tearDown() {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource!!)
        }
    }

    /**
     * Test case no: TC0114
     * Test case name: Tap erase private browsing notification when private browsing
     * Steps:
     * 1. Visit site in private mode
     * 2. Open notification
     * 3. Tap erase private browsing
     * 4. Open notification
     * 5. Check no erase private browsing notification displayed
     */
    @Test
    fun tapErasePrivateBrowsingNotif_whenPrivateBrowsing() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
        }

        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_erase_message)
            openNotification()
            checkNotificationNotDisplayed(activityTestRule.activity, R.string.private_browsing_erase_message)
        }
    }

    /**
     * Test case no: TC0117
     * Test case name: Tap erase private browsing notification when normal browsing
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap private mode icon so back to normal browsing
     * 3. Open notification
     * 4. Tap erase private browsing
     * 5. Open notification
     * 6. Check no erase private browsing notification displayed
     */
    @Test
    fun tapErasePrivateBrowsingNotif_whenNormalBrowsing() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
        }

        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_erase_message)
            openNotification()
            checkNotificationNotDisplayed(activityTestRule.activity, R.string.private_browsing_erase_message)
        }
    }

    /**
     * Test case no: TC0112
     * Test case name:  tap open action notification when private browsing
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap private mode icon so back to normal browsing
     * 2. Open notification
     * 3. Tap open
     * 4. Check private site url matches target
     */
    @Test
    fun tapOpenActionNotif_whenPrivateBrowsing() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
            tapBackToBrowserInPrivateMenu()
        }

        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_open_action)
        }

        private {
            checkUrlInPrivateMode(PrivateModeNotification.TARGET_URL_SITE)
        }
    }

    /**
     * Test case no: TC0115
     * Test case name: tap open action notification when normal browsing
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap private mode icon so back to normal browsing
     * 3. Open notification
     * 4. Tap open
     * 5. Check private site url matches target
     */
    @Test
    fun tapOpenActionNotif_whenNormalBrowsing() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
            tapBackToBrowserInPrivateMenu()
        }

        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_open_action)
        }

        private {
            checkUrlInPrivateMode(PrivateModeNotification.TARGET_URL_SITE)
        }
    }

    /**
     * Test case no: TC0118
     * Test case name: Tap open action notification after switch to home
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap private mode icon so back to normal browsing
     * 3. Tap home
     * 4. Open notification
     * 5. Tap open
     * 6. Check private site url matches target
     */
    @Test
    fun tapOpenActionNotif_afterSwitchToHome() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
            tapBackToBrowserInPrivateMenu()
        }

        session {
            goHome(activityTestRule.activity)
        }

        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_open_action)
        }

        private {
            checkUrlInPrivateMode(PrivateModeNotification.TARGET_URL_SITE)
        }
    }

    /**
     * Test case no: TC0120
     * Test case name: Tap erase private browsing notification after switch to home
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap private mode icon so back to normal browsing
     * 4. Tap home
     * 5. Open notification
     * 6. Tap erase private browsing
     * 7. Open notification
     * 8. Check no erase private browsing notification displayed
     */
    @Test
    fun tapErasePrivateBrowsingNotif_afterSwitchToHome() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
            tapBackToBrowserInPrivateMenu()
        }

        session {
            goHome(activityTestRule.activity)
        }
        notification {
            openNotification()
            clickNotificationWithText(activityTestRule.activity, R.string.private_browsing_erase_message)
            openNotification()
            checkNotificationNotDisplayed(activityTestRule.activity, R.string.private_browsing_erase_message)
        }
    }

    /**
     * Test case no: TC0122
     * Test case name: Show erase private browsing toast when back to private home page
     * Steps:
     * 1. Visit site in private mode
     * 2. Tap back
     * 3. Show private browsing notification toast
     */
    @Test
    fun showPrivateBrowsingErasedToast_afterbacktoPrivateHomePage() {

        private {
            loadPageFromPrivateSearchField(activityTestRule.activity, PrivateModeNotification.TARGET_URL_SITE)
        }

        session {
            pressBack()
        }

        private {
            showToastMessageInPrivateMode(activityTestRule.activity, R.string.private_browsing_erase_done)
        }
    }

    companion object {
        private val TARGET_URL_SITE = "file:///android_asset/gpl.html"
    }
}