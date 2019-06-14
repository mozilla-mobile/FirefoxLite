/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.annotation.Keep
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.autobot.bottomBar
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.helper.SessionLoadedIdlingResource
import org.mozilla.focus.utils.AndroidTestUtils

@Ignore
@Keep
@RunWith(AndroidJUnit4::class)
class ScreenOrientationTest {
    companion object {
        private const val TARGET_URL_SITE_1 = "file:///android_asset/gpl.html"
    }

    @Rule @JvmField
    val activityTestRule = ActivityTestRule(
            MainActivity::class.java,
            true,
            false
    )

    private var sessionLoadedIdlingResource: SessionLoadedIdlingResource? = null

    @Before
    fun setUp() {
        BeforeTestTask.Builder().clearBrowsingHistory().build().execute()
        activityTestRule.launchActivity(Intent())
    }

    @After
    fun tearDown() {
        sessionLoadedIdlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
    }

    @Test
    fun testBrowserScreen() {
        // Prepare
        gotoBrowserScreen()

        // Test - Should be SCREEN_ORIENTATION_USER when in browser screen
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)
    }

    @Test
    fun testFirstLevelMenu() {
        // Prepare
        gotoBrowserScreen()
        AndroidTestUtils.tapBrowserMenuButton()

        // Test - Should be SCREEN_ORIENTATION_PORTRAIT when the first level menu is opened
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)

        Espresso.pressBack()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)

        assertBackToHomeOrientation()
    }

    @Test
    fun testSecondLevelMenu() {
        // Prepare
        gotoBrowserScreen()

        AndroidTestUtils.tapBrowserMenuButton()
        onView(withId(R.id.menu_history)).perform(click())

        // Test - Should be SCREEN_ORIENTATION_PORTRAIT when the second level menu is opened
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)

        Espresso.pressBack()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)

        assertBackToHomeOrientation()
    }

    @Test
    fun testFindInPage() {
        // Prepare
        gotoBrowserScreen()
        AndroidTestUtils.tapBrowserMenuButton()

        onView(withId(R.id.menu_find_in_page)).perform(click())

        // Test - Should be SCREEN_ORIENTATION_PORTRAIT when in find-in-page mode
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)

        Espresso.pressBack()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)

        assertBackToHomeOrientation()
    }

    @Test
    fun testTabTray() {
        // Prepare
        gotoBrowserScreen()
        bottomBar {
            clickBrowserBottomBarItem(R.id.bottom_bar_tab_counter)
        }

        // Test - Should be SCREEN_ORIENTATION_PORTRAIT when the second level menu is opened
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)

        Espresso.pressBack()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)

        assertBackToHomeOrientation()
    }

    @Test
    fun testUrlInput() {
        // Prepare
        gotoBrowserScreen()
        onView(withId(R.id.display_url)).perform(click())

        // Test - Should be SCREEN_ORIENTATION_PORTRAIT when the url fragment is presented
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)

        onView(withId(R.id.dismiss)).perform(click())

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, activityTestRule.activity.requestedOrientation)

        assertBackToHomeOrientation()
    }

    private fun assertBackToHomeOrientation() {
        Espresso.pressBack()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityTestRule.activity.requestedOrientation)
    }

    private fun gotoBrowserScreen() {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource(activityTestRule.activity)

        onView(withId(R.id.home_fragment_fake_input))
                .perform(click())
        onView(withId(R.id.url_edit)).check(matches(isDisplayed()))
        onView(withId(R.id.url_edit)).perform(replaceText(TARGET_URL_SITE_1), pressImeActionButton())

        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource)

        onView(withId(R.id.display_url)).check(matches(isDisplayed()))

        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource)
    }
}
