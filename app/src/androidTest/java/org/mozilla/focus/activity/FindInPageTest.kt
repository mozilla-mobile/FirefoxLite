/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import android.support.annotation.Keep
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.action.ViewActions.swipeUp
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.autobot.findInPage
import org.mozilla.focus.autobot.session
import org.mozilla.focus.utils.AndroidTestUtils

@Keep
@RunWith(AndroidJUnit4::class)
class FindInPageTest {

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
        private const val TARGET_URL_SITE = "file:///android_asset/gpl.html"
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
    @Test
    fun findInPageAndScroll() {

        val keyword = "program"

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            findKeywordInPage(keyword)
        }

        onView(withId(R.id.webview_slot)).perform(swipeUp())
        onView(withId(R.id.webview_slot)).perform(swipeDown())
        onView(withId(R.id.find_in_page_query_text)).check(matches(withText(keyword)))
        onView(withId(R.id.find_in_page_result_text)).check(matches(isDisplayed()))

        findInPage {
            findKeywordInPage("program")
            closeButtonForFindInPage()
        }

        onView(withId(R.id.find_in_page)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    /**
     * Test case no: TC0203
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
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        findInPage {
            findKeywordInPage("program")
            navigateKeywordSearchInPage(true)
            navigateKeywordSearchInPage(true)
            navigateKeywordSearchInPage(false)
            navigateKeywordSearchInPage(false)
        }
    }

    /**
     * Test case no: TC0207
     * Test case name: Close "Find in page" mode via system Back key
     * 1. Launch Rocket
     * 2. Visit mozilla.org
     * 3. Tap Menu -> Find in page
     * 4. Tap system Back key twice"
     */
    @Test
    fun closeFindInPageWithSystemBackKey() {

        session {
            loadPageFromHomeSearchField(activityTestRule.activity, TARGET_URL_SITE)
            clickBrowserMenu()
            clickMenuFindInPage()
        }

        onView(withId(R.id.find_in_page)).perform(click())

        findInPage {
            findKeywordInPage("program")
        }

        Espresso.pressBack()
        Espresso.pressBack()

        onView(withId(R.id.find_in_page)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    /**
     * Test case no: TC0209
     * Test case name: Open a link from external app then close mode
     * 1. Launch Rocket
     * 2. Visit google.com
     * 3. Tap Menu -> Find in page
     * 4. Open a link from external app
     */
    @Test
    @Ignore
    fun closeFindInPageWithExternalLink() {
    }

    /**
     * Test case no: TC0210
     * Test case name: Check mini url bar and the bottom action in "Find in page" mode
     * 1. Launch Rocket
     * 2. Login facebook.com with your account
     * 3. Tap Menu -> Find in page
     * 4. Find the word
     * 5. Scroll up and down
     */
    /**
     * Tese case no: TC0215
     * Test case name: When the keyboard is on, tap Home->Rocket, the keyboard should be opened
     * 1. Launch Rocket
     * 2. Visit a website
     * 3. Tap Menu -> Find in page
     * 4. Tap Home
     * 5. Tap Rocket again
     */
    /**
     * Test case no: TC0218
     * Test case name: When user scrolls up/down in Find in page mode, it would not close the keyboard
     * 1. Launch Rocket
     * 2. Visit facebook.com with your account
     * 3. Tap Menu -> Find in page
     * 4. Scroll up
     * 5. Scroll down
     */
    @Test
    @Ignore
    fun keyboardBehaviorForFindInPage() {
    }
}
