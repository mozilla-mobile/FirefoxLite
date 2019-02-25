package org.mozilla.focus.autobot

import android.content.Intent
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.DataInteraction
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.IdlingResource
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasAction
import android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra
import android.support.test.espresso.intent.matcher.IntentMatchers.hasType
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.v7.widget.RecyclerView
import android.widget.Switch
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.AllOf
import org.hamcrest.core.Is
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Rule
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.helper.ActivityRecreateLeakWatcherIdlingResource
import org.mozilla.focus.screenshot.ScreenshotCaptureTask
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.screenshot.model.Screenshot
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.widget.TelemetrySwitchPreference
import org.mozilla.focus.widget.TurboSwitchPreference
import java.io.IOException

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

class ScreenshotRobot : MenuRobot() {

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

    fun clickAndDeleteTheFirstItemInMyShots() {

        // Delete the screenshot
        onView(withId(R.id.screenshot_viewer_btn_delete)).perform(click())

        // Confirm delete
        onView(allOf(withText(R.string.browsing_history_menu_delete), isDisplayed())).perform(click())

        // FIXME: Add an idling resource here between delete and isDisplayed check
        // Check if come back to my shots panel
        onView(withId(R.id.screenshots)).check(matches(isDisplayed()))
    }

    fun clickInfoTheFirstItemInMyShots() {

        // Click info
        onView(withId(R.id.screenshot_viewer_btn_info)).perform(click())

        // Check image viewer dialog is displayed
        onView(withText(R.string.screenshot_image_viewer_dialog_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
    }

    fun checkScreenshotInfoDialogDimissed() {

        // Check image viewer dialog is dimissed
        onView(withText(R.string.screenshot_image_viewer_dialog_title))
                .check(ViewAssertions.doesNotExist())
    }

    fun clickOpenWebInScreenshotViewer(url: String) {

        // Click open web
        onView(withId(R.id.screenshot_viewer_btn_open_url)).perform(click())

        //Check Url displayed
        onView(AllOf.allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(url)))
    }

    fun clickShareInScreenshotViewer() {

        // Click share
        onView(AllOf.allOf(withId(R.id.screenshot_viewer_btn_share), isDisplayed())).perform(click())

        // Check share intent is sent
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT), allOf(hasAction(Intent.ACTION_SEND), hasType("image/*")))))
    }

    fun clickEditInScreenshotViewer() {

        // Click edit
        onView(withId(R.id.screenshot_viewer_btn_edit)).perform(click())

        // Check edit intent is sent
        intended(allOf<Intent>(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`<String>(Intent.EXTRA_INTENT), allOf<Intent>(hasAction(Intent.ACTION_EDIT)))))
    }

    companion object {

        private val TITLE = "PHOTO_TITLE"
        private val URL = "/site2/"
        private const val TIMESTAMP = 9999
    }

    @Throws(IOException::class)
    fun insertScreenshotToDatabase(): Screenshot {

        // make sample bitmap
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = BitmapFactory.decodeResource(context.resources,
                R.drawable.first_run_upgrade_image1)

        // save bitmap to storage
        val path = ScreenshotCaptureTask.saveBitmapToStorage(context, "Screenshot_test", icon)

        // notify media scanner
        FileUtils.notifyMediaScanner(context, path)

        // create screenshot
        val screenshot = Screenshot(TITLE, URL, TIMESTAMP.toLong(), path)
        ScreenshotManager.getInstance().insert(screenshot, null)
        return screenshot
    }

    @Throws(IOException::class)
    fun deleteScreenshotFromDatabase(screenshot: Screenshot) {
        ScreenshotManager.getInstance().delete(screenshot.id, null)
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
