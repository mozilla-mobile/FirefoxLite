package org.mozilla.focus.screengrab

import android.content.Intent
import android.os.SystemClock
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.PreferenceMatchers
import android.support.test.espresso.matcher.PreferenceMatchers.withKey
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withParent
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.annotation.ScreengrabOnly
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.utils.AndroidTestUtils
import tools.fastlane.screengrab.FalconScreenshotStrategy
import tools.fastlane.screengrab.Screengrab

@ScreengrabOnly
@RunWith(AndroidJUnit4::class)
class NewsScreenshot : BaseScreenshot() {

    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        BeforeTestTask.Builder()
                .enableContentPortalNews(true)
                .build()
                .execute()
        activityTestRule.launchActivity(Intent())
        Screengrab.setDefaultScreenshotStrategy(FalconScreenshotStrategy(activityTestRule.activity))
    }

    @Test
    fun portalNewsSettings() {

        // Tap home menu -> settings
        AndroidTestUtils.tapHomeMenuButton()

        onView(withId(R.id.menu_preferences)).perform(click())

        // Check news source displayed
        onView(allOf(withText(R.string.menu_settings), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()))
        Screengrab.screenshot(ScreenshotNamingUtils.PORTAL_NEWS_SOURCE)

        // Tap news source
        val resources = activityTestRule.getActivity().getResources()
        onData(withKey(resources.getString(R.string.pref_s_news))).perform(click())

        // Check news source dialog pop up
        onView(withText(R.string.preference_dialog_title_news_source)).inRoot(isDialog()).check(matches(isDisplayed()))
        Screengrab.screenshot(ScreenshotNamingUtils.PORTAL_NEWS_SOURCE_DIALOG)

        // Tap back
        Espresso.pressBack()

        // Tap about to see learn more about life feed
        onData(PreferenceMatchers.withKey(resources.getString(R.string.pref_key_about))).perform(click())
        onView(withText(R.string.menu_about)).check(matches(isDisplayed()))
        SystemClock.sleep(MockUIUtils.SHORT_DELAY.toLong())
        Screengrab.screenshot(ScreenshotNamingUtils.PORTAL_NEWS_ABOUT_LIFE_FEED)
    }
}