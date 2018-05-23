package org.mozilla.focus.autobot

import android.content.Intent
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.DataInteraction
import android.support.test.espresso.Espresso
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.IdlingResource
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.rule.ActivityTestRule
import android.widget.Switch
import junit.framework.Assert
import org.hamcrest.CoreMatchers
import org.hamcrest.core.Is
import org.junit.Rule
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.helper.ActivityRecreateLeakWatcherIdlingResource
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.widget.TelemetrySwitchPreference

inline fun setting(func: SettingRobot.() -> Unit) = SettingRobot().apply(func)

inline fun runWithIdleRes(ir: IdlingResource?, pendingCheck: () -> Unit) {

    IdlingRegistry.getInstance().register(ir)
    pendingCheck()
    IdlingRegistry.getInstance().unregister(ir)

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

