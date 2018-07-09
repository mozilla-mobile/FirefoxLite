package org.mozilla.rocket.privately

import android.preference.PreferenceManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class PrivateModeTest {

    @Test
    fun `Private mode should be disable if it's pref off`() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(PrivateMode.PREF_KEY_PRIVATE_MODE_ENABLED, true)
                .apply()
        assertTrue(PrivateMode.isEnable(RuntimeEnvironment.application))
    }
}