/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.utils.AndroidTestUtils
import org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition
import org.mozilla.focus.utils.visibleWithId
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.home.topsites.ui.Site

@Keep
@RunWith(AndroidJUnit4::class)
class RemoveTopSitesTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    private lateinit var siteList: List<Site>
    private lateinit var context: Context
    private lateinit var removeLabel: String

    @Before
    @Throws(JSONException::class)
    fun setUp() {
        AndroidTestUtils.beforeTest()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        removeLabel = context.getString(R.string.remove)
        prepareTopSiteList()
        activityTestRule.launchActivity(Intent())
    }

    /**
     * Test case no: TC0086
     * Test case name: One top site deleted
     * Steps:
     * 1. Launch app
     * 2. long click to delete top site
     */
    @Test
    fun deleteTopSite_deleteSuccessfully() {

        // Pick a test site to delete
        val siteIndex = 4
        val testSite = siteList[siteIndex] as Site.UrlSite

        onView(withId(R.id.main_list)).check(matches(isDisplayed()))

        // Check the title of test site is matched
        onView(visibleWithId(R.id.page_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.title)))))

        // Long click the test site
        onView(visibleWithId(R.id.page_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(siteIndex, longClick()))

        // Check the remove button is displayed
        onView(withText(removeLabel)).check(matches(isDisplayed()))

        // Click the remove button
        onView(withText(removeLabel))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click())

        // Check the test site is removed
        onView(visibleWithId(R.id.page_list))
                .check(matches(not(atPosition(siteIndex, hasDescendant(withText(testSite.title))))))
    }

    /**
     * Test case no: TC0172
     * Test case name: cancel removing top site action
     * Steps:
     * 1. Launch app
     * 2. Long click to remove one top site
     * 3. Press back key
     */
    @Test
    fun deleteTopSiteAndCancel_topSiteIsStillThere() {

        // Pick a test site to test
        val siteIndex = 4
        val testSite = siteList[siteIndex] as Site.UrlSite

        onView(withId(R.id.main_list)).check(matches(isDisplayed()))

        // Check the title of test site is matched
        onView(visibleWithId(R.id.page_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.title)))))

        // Long click the test site
        onView(visibleWithId(R.id.page_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(siteIndex, longClick()))

        // Check the remove button is displayed
        onView(withText(removeLabel))
                .check(matches(isDisplayed()))

        // Press the back key
        Espresso.pressBack()

        // Check the title of test site is matched
        onView(visibleWithId(R.id.page_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.title)))))
    }

    private fun prepareTopSiteList() {
        siteList = runBlocking {
            (getApplicationContext() as Context).appComponent()
                .getTopSitesUseCase().invoke()
        }

        Assert.assertNotNull(siteList)
        Assert.assertTrue(siteList.isNotEmpty())
    }
}