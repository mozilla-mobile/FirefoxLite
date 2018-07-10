package org.mozilla.rocket.privately

import android.preference.PreferenceManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mozilla.rocket.tabs.Tab
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class PrivateModeTest {

    @Test
    fun `Private mode should be disable if it's pref off`() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(PrivateMode  .PREF_KEY_PRIVATE_MODE_ENABLED, true)
                .apply()
        assertTrue(PrivateMode.isEnable(RuntimeEnvironment.application))
    }

    @Test
    fun `clearFootprint() shouldn't be called if at least one registered`() {
        val mockSanitizer = mock(PrivateModeListener::class.java)
        val privateMode = PrivateMode(mockSanitizer)

        val mockComponent = mock(Tab::class.java)
        `when`(mockComponent.id).thenReturn("")
        val mockFootprint = mock(Footprint::class.java)

        privateMode.register(mockComponent, mockFootprint)
        verify(mockSanitizer, times(0)).onZeroObserver(mockFootprint)

        privateMode.unregister(mockComponent)
        verify(mockSanitizer, times(1)).onZeroObserver(mockFootprint)

    }
}