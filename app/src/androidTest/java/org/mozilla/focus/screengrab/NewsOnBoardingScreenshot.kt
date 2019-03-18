package org.mozilla.focus.screengrab

import android.content.Intent
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isChecked
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
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
import tools.fastlane.screengrab.FalconScreenshotStrategy
import tools.fastlane.screengrab.Screengrab

@ScreengrabOnly
@RunWith(AndroidJUnit4::class)
class NewsOnBoardingScreenshot : BaseScreenshot() {

    @Rule
    @JvmField
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        BeforeTestTask.Builder()
                .setSkipFirstRun(false)
                .setSkipColorThemeOnBoarding(false)
                .enableContentPortalNews(true)
                .build()
                .execute()
        activityTestRule.launchActivity(Intent())
        Screengrab.setDefaultScreenshotStrategy(FalconScreenshotStrategy(activityTestRule.activity))
    }

    @Test
    fun newsOnBoarding() {

        // Check if turbo mode switch is on
        onView(allOf(withId(R.id.switch_widget), isDisplayed())).check(matches(isChecked()))

        // Click next button in the first on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click())

        // Click next button in the second on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click())

        // Click next button in the third on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click())

        // Click next button in the forth on boarding page
        onView(allOf(withId(R.id.next), isDisplayed())).perform(click())

        // Click next button in the third on boarding page
        Screengrab.screenshot(ScreenshotNamingUtils.PORTAL_NEWS_ONBOARDING)
    }
}