/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Intent
import android.net.Uri
import android.support.annotation.Keep
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.action.ViewActions.swipeUp
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.autobot.findInPage
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.helper.SessionLoadedIdlingResource

@Keep
@RunWith(AndroidJUnit4::class)
class FindInPageTest {

    @JvmField
    @Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        BeforeTestTask.Builder()
                .enableSreenshotOnBoarding(true)
                .build()
                .execute()
        activityTestRule.launchActivity(Intent())
    }

    companion object {
        private const val TARGET_URL_SITE_1 = "file:///android_asset/gpl.html"
        private const val TARGET_URL_SITE_2 = "https://developer.mozilla.org/en-US/Firefox_for_Android"
        private const val keyword = "program"
    }

    /**
     * Test case no: TC0147
     * Test case name: Find a specific word or phrase on a page
     * Steps
     * 1. Launch Rocket
     * 2. Visit mozilla.org
     * 3. Tap Menu -> Find in page
     * 4. Find a specific word or phrase
     * 5. Scroll up and down
     */
    /**
     * Test case no: TC0154
     * Test case name: Check mini url bar and the bottom action in "Find in page" mode
     * 1. Launch Rocket
     * 2. Login facebook.com with your account
     * 3. Tap Menu -> Find in page
     * 4. Find the word
     * 5. Scroll up and down
     */
    @Test
    fun findInPageAndScroll() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE_1)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            findKeywordInPage(keyword)

            onView(withId(R.id.webview_slot)).perform(swipeUp())
            onView(withId(R.id.webview_slot)).perform(swipeDown())
            onView(withId(R.id.find_in_page_query_text)).check(matches(withText(keyword)))
            onView(withId(R.id.find_in_page_result_text)).check(matches(isDisplayed()))

            onView(withId(R.id.display_url)).check(matches(isDisplayed()))
            onView(withId(R.id.browser_screen_menu)).check(matches(isDisplayed()))

            onView(withId(R.id.find_in_page_close_btn)).check(matches(isDisplayed())).perform(click())

            onView(withId(R.id.find_in_page)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        }
    }

    /**
     * Test case no: TC0148
     * Test case name: Check the finding visibilities
     * Steps:
     * 1. Launch Rocket
     * 2. Visit a webpage
     * 3. Tap Menu -> Find in page
     * 4. Search a word
     * 5. Tap up and down next button on search field"
     * */
    @Test
    fun findInPageWhenSearchKeyword() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE_1)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            findKeywordInPage(keyword)

            checkFindInPageToolBarIsDisplayed()

            navigateKeywordSearchInPage(true)
            navigateKeywordSearchInPage(true)
            navigateKeywordSearchInPage(false)
            navigateKeywordSearchInPage(false)
        }
    }

    /**
     * Test case no: TC0152
     * Test case name: Close "Find in page" mode via system Back key
     * 1. Launch Rocket
     * 2. Visit mozilla.org
     * 3. Tap Menu -> Find in page
     * 4. Tap system Back key twice"
     */
    @Test
    fun closeFindInPageWithSystemBackKey() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE_1)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        onView(withId(R.id.find_in_page)).perform(click())

        findInPage {
            findKeywordInPage(keyword)

            Espresso.pressBack()
            Espresso.pressBack()

            onView(withId(R.id.find_in_page)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        }
    }

    /**
     * Test case no: TC0153
     * Test case name: Open a link from external app then close mode
     * 1. Launch Rocket
     * 2. Visit google.com
     * 3. Tap Menu -> Find in page
     * 4. Open a link from external app
     */
    @Test
    fun closeFindInPageWithExternalLink() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE_1)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            onView(withId(R.id.find_in_page)).perform(click())

            assertTrue(isKeyboardShown())
            findKeywordInPage(keyword)
        }

        val loadingIdlingResource = SessionLoadedIdlingResource(activityTestRule.activity)
        IdlingRegistry.getInstance().register(loadingIdlingResource)
        sendBrowsingIntent()

        onView(withId(R.id.find_in_page)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(R.id.display_url)).check(matches(isDisplayed()))
        onView(withId(R.id.display_url)).check(matches(withText(TARGET_URL_SITE_2)))

        IdlingRegistry.getInstance().unregister(loadingIdlingResource)
    }

    /**
     * Tese case no: TC0156
     * Test case name: When the keyboard is on, tap Home->Rocket, the keyboard should be opened
     * 1. Launch Rocket
     * 2. Visit a website
     * 3. Tap Menu -> Find in page
     * 4. Tap Home
     * 5. Tap Rocket again
     */
    /**
     * Test case no: TC0158
     * Test case name: When user scrolls up/down in Find in page mode, it would not close the keyboard
     * 1. Launch Rocket
     * 2. Visit facebook.com with your account
     * 3. Tap Menu -> Find in page
     * 4. Scroll up
     * 5. Scroll down
     */
    @Test
    fun keyboardBehaviorForFindInPage() {
        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE_1)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            onView(withId(R.id.find_in_page_query_text)).perform(click())
        }

        session {
            goHome(activityTestRule.activity)
            bringToForeground(activityTestRule.activity)
        }

        findInPage {
            assertTrue(isKeyboardShown())

            onView(withId(R.id.webview_slot)).perform(swipeUp())
            onView(withId(R.id.webview_slot)).perform(swipeDown())

            assertTrue(isKeyboardShown())
        }
    }
    
    private fun sendBrowsingIntent() {
        // Simulate third party app sending browsing url intent to rocket
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(TARGET_URL_SITE_2)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        intent.setPackage(targetContext.packageName)
        targetContext.startActivity(intent)
    }
}
