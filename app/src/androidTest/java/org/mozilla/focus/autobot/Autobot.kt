package org.mozilla.focus.autobot

import android.Manifest
import android.content.Intent
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.DataInteraction
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.IdlingResource
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Switch
import org.junit.Assert
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.Is
import org.junit.Rule
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.helper.ActivityRecreateLeakWatcherIdlingResource
import org.mozilla.focus.helper.ScreenshotIdlingResource
import org.mozilla.focus.helper.SessionLoadedIdlingResource
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.widget.TelemetrySwitchPreference

inline fun setting(func: SettingRobot.() -> Unit) = SettingRobot().apply(func)
inline fun session(func: SessionRobot.() -> Unit) = SessionRobot().apply(func)

inline fun runWithIdleRes(ir: IdlingResource?, pendingCheck: () -> Unit) {

    IdlingRegistry.getInstance().register(ir)
    pendingCheck()
    IdlingRegistry.getInstance().unregister(ir)
}

class SessionRobot {
    @Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Rule
    val writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Rule
    val readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    private var sessionLoadedIdlingResource: SessionLoadedIdlingResource? = null

    // Load and check if the test site is loaded
    fun loadPage(url: String) {
        activityTestRule.launchActivity(Intent())

        sessionLoadedIdlingResource = SessionLoadedIdlingResource(activityTestRule.getActivity())

        // Click search field
        onView(allOf<View>(withId(R.id.home_fragment_fake_input), isDisplayed())).perform(click())

        // Enter test site url
        onView(allOf<View>(withId(R.id.url_edit), isDisplayed())).perform(replaceText(url), pressImeActionButton())

        IdlingRegistry.getInstance().register(sessionLoadedIdlingResource)

        onView(allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(url)))

        IdlingRegistry.getInstance().unregister(sessionLoadedIdlingResource)
    }

    infix fun takeScreenshot(func: ScreenshotRobot.() -> Unit) {

        ScreenshotRobot(activityTestRule).takeScreenshot().apply { func() }
    }
}

class ScreenshotRobot(val activityTestRule: ActivityTestRule<MainActivity>) {

    private var screenshotIdlingResource: ScreenshotIdlingResource? = null

    fun takeScreenshot(): ScreenshotRobot {
        screenshotIdlingResource = ScreenshotIdlingResource(activityTestRule.getActivity())

        // Click screen capture button
        onView(allOf(withId(R.id.btn_capture), isDisplayed())).perform(click())

        // Register screenshot taken idling resource and wait capture complete
        IdlingRegistry.getInstance().register(screenshotIdlingResource)

        // wait for the screen shot to complete
        // Open menu
        onView(allOf(withId(R.id.btn_menu), isDisplayed())).perform(click())

        IdlingRegistry.getInstance().unregister(screenshotIdlingResource)
        return this
    }

    fun clickMenuMyShots() {
        // Click my shot
        onView(allOf(withId(R.id.menu_screenshots), isDisplayed())).perform(click())
    }

    fun clickFirstItemInMyShotsAndOpen() {
        // Click the first item in my shots panel
        // Since "index=0" in ScreenshotItemAdapter is always date label, the first screenshot item will start from "index=1".
        onView(withId(R.id.screenshot_grid_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Check if screenshot is displayed
        onView(withId(R.id.screenshot_viewer_image)).check(matches(isDisplayed()))

        // Check if open url/edit/share/info/delete button is there
        onView(withId(R.id.screenshot_viewer_btn_open_url)).check(matches(isDisplayed()))
        onView(withId(R.id.screenshot_viewer_btn_edit)).check(matches(isDisplayed()))
        onView(withId(R.id.screenshot_viewer_btn_share)).check(matches(isDisplayed()))
        onView(withId(R.id.screenshot_viewer_btn_info)).check(matches(isDisplayed()))
        onView(withId(R.id.screenshot_viewer_btn_delete)).check(matches(isDisplayed()))
    }

    fun longClickAndDeleteTheFirstItemInMyShots() {

        // Delete the screenshot
        onView(withId(R.id.screenshot_viewer_btn_delete)).perform(click())

        // Confirm delete
        onView(allOf(withText(R.string.browsing_history_menu_delete), isDisplayed())).perform(click())

        // Check if come back to my shots panel
        onView(withId(R.id.screenshots)).check(matches(isDisplayed()))
    }
}

class SettingRobot {

    @Rule
    @JvmField
    val settingsActivity = ActivityTestRule(SettingsActivity::class.java, false, false)

    private lateinit var interaction: DataInteraction

    private lateinit var leakWatchIdlingResource: ActivityRecreateLeakWatcherIdlingResource

    init {
        // make sure the pref is on when started
        resetPref()

        settingsActivity.launchActivity(Intent())

        FirebaseHelper.init(settingsActivity.activity, true)

        FirebaseHelper.injectEnablerCallback(Delay())
    }

    fun isChecked(): SettingRobot {
        interaction.check(ViewAssertions.matches(ViewMatchers.isChecked()))
        return this
    }

    fun isNotChecked(): SettingRobot {
        interaction.check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
        return this
    }

    fun click(): SettingRobot {
        interaction.perform(ViewActions.click())

        return this
    }

    fun prefSendUsageData(): SettingRobot {
        // Click on the switch multiple times...
        interaction = Espresso.onData(
                Is.`is`(CoreMatchers.instanceOf(TelemetrySwitchPreference::class.java))).onChildView(ViewMatchers.withClassName(Is.`is`(Switch::class.java.name)))
        return this
    }

    fun restartAndcheckNoLeak(): SettingRobot {
        // shouldn't leak SettingsActivity if SettingsActivity is recreated before the task completed.
        leakWatchIdlingResource = ActivityRecreateLeakWatcherIdlingResource(settingsActivity.activity)

        // re create the activity to force the current one to call onDestroy.
        // two things are happening here:
        // 1. the dying one is being tracked
        // 2. when the new activity is created, idling resource will check if the old one is cleared.
        settingsActivity.activity.runOnUiThread { settingsActivity.activity.recreate() }

        // leakWatchIdlingResource will be idle is gc is completed.
        runWithIdleRes(leakWatchIdlingResource) {
            // call onView to sync and wait for idling resource
            Espresso.onView(ViewMatchers.isRoot())
            Assert.assertFalse(leakWatchIdlingResource.hasLeak())
        }

        return this
    }

    private fun resetPref() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefName = context.getString(R.string.pref_key_telemetry)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(prefName, true).apply()
    }

    class Delay : FirebaseHelper.BlockingEnablerCallback {
        override fun runDelayOnExecution() {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
