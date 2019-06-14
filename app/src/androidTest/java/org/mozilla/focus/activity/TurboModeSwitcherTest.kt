package org.mozilla.focus.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.setting
import org.mozilla.focus.utils.AndroidTestUtils

@RunWith(AndroidJUnit4::class)
class TurboModeSwitcherTest {

    @Before
    fun setup() {
        AndroidTestUtils.beforeTest()
    }

    /**
     * Test case no: TC0043
     * Test case name: Turbo mode on/ off in settings
     * Steps:
     * 1. Launch app
     * 2. Tap settings
     * 3. Check turbo mode checked
     * 4. Tap turbo mode
     * 5. Check turbo mode is not checked
     */
    @Test
    fun turboMode() {

        setting {
            // check initial state
            prefTurboMode()
            isChecked()
            click()
            isNotChecked()
            // Set turbo mode back to on
            click()
        }
    }
}