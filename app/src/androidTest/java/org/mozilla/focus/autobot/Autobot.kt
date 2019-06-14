package org.mozilla.focus.autobot

import android.content.Intent
import android.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.recyclerview.widget.RecyclerView
import android.widget.Switch
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Rule
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.helper.ActivityRecreateLeakWatcherIdlingResource
import org.mozilla.focus.widget.TelemetrySwitchPreference
import org.mozilla.focus.widget.TurboSwitchPreference

inline fun setting(func: SettingRobot.() -> Unit) = SettingRobot().apply(func)
inline fun screenshot(func: ScreenshotRobot.() -> Unit) = ScreenshotRobot().apply(func)

inline fun runWithIdleRes(ir: IdlingResource?, pendingCheck: () -> Unit) {
    try {
        IdlingRegistry.getInstance().register(ir)
        pendingCheck()
    } finally {
        IdlingRegistry.getInstance().unregister(ir)
    }
}

class ScreenshotRobot(menuAutomation: MenuAutomation = MenuRobot()) : MenuAutomation by menuAutomation {

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

        // FIXME: Add an idling resource here between delete and isDisplayed check
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

    fun prefTurboMode(): SettingRobot {
        // Click on the switch multiple times...
        interaction = Espresso.onData(
                Is.`is`(CoreMatchers.instanceOf(TurboSwitchPreference::class.java))).onChildView(ViewMatchers.withClassName(Is.`is`(Switch::class.java.name)))
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
}
