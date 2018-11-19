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

        setting {
            // check initial state
            prefSendUsageData()
            isChecked()

            // click and see the result
            click()
            isNotChecked()
        }
    }

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
}