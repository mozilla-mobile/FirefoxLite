package org.mozilla.focus.activity


import android.support.test.filters.SdkSuppress
import android.support.test.runner.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.setting
import org.mozilla.focus.utils.AndroidTestUtils


// Only device with API>=24 can set default browser via system settings
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
class FirebaseSwitcherTest {

    @Before
    fun setup() {
        AndroidTestUtils.beforeTest()
    }


    @Test
    fun disableFirebase_makeSureSwitchIsOffAfterClick() {

        // SettingRobot().apply {
        setting {
            // check initial state
            prefSendUsageData()
            isChecked()

            // click and see the result
            click()
            isNotChecked()
        }
    }

//
//    /**
//     *  This is an integration test (UI test on real component) so no need to use robot
//     *  It calls bind() multiple times to see if any runnable is created.
//     *   no actual click was performed. This test case here is not really a UI test.
//     */
//    @Test
//    fun callBindCrazily_OnlyOneRunnableIsCreated() {
//
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//
//        // I've added some latency to the enabler, in case it runs too fast
//        FirebaseHelper.injectEnablerCallback(SettingRobot.Delay())
//
//        val enable = TelemetryWrapper.isTelemetryEnabled(context)
//        // The state is not changed, but we still want keep the same status and call bind again to kick off the enabler.
//        // I use bind() to simulate the click. This is because calling click can't let get the return value
//        // of the bind method ( I use it to determine if a new Runnable is created)
//        var newRunnableCreated = FirebaseHelper.bind(context, enable)
//        // Only this time will be true
//        // first time, should get true from bind
//        assertTrue(newRunnableCreated)
//
//        // the successors should return false( not create new runnable)
//        // assume below three method calls happen very fast
//        newRunnableCreated = FirebaseHelper.bind(context, enable)
//        assertFalse(newRunnableCreated)
//
//        newRunnableCreated = FirebaseHelper.bind(context, enable)
//        // second time, should also get false
//        assertFalse(newRunnableCreated)
//
//        newRunnableCreated = FirebaseHelper.bind(context, enable)
//        // third time, should still get false
//        assertFalse(newRunnableCreated)
//
//    }

    @Test
    fun flipPrefCrazily_TheStateIsSynced() {

        // SettingRobot().apply {
        setting {
            // check initial state
            prefSendUsageData()
            isChecked()

            // now flip crazily and check the result
            click() // on -> off
            click() // off -> on
            click() // on -> off
            click() // off -> on
            isChecked()
        }
    }

    @Test
    fun flipAndLeave_ShouldHaveNoLeak() {

        // SettingRobot().apply {
        setting {
            // check initial state
            prefSendUsageData()
            isChecked()

            // now flip crazily and check for leak
            click() // on -> off
            click() // off -> on
            click() // on -> off
            click() // off -> on
            restartAndcheckNoLeak()
        }
    }

}