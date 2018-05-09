package org.mozilla.focus.telemetry

import android.content.Intent
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mozilla.focus.utils.SafeIntent

class AppLaunchMethodTest {

    @Test
    fun launcherIconIntent_parse_launcherMode() {
        val safeIntent = mock(SafeIntent::class.java)
        Mockito.`when`(safeIntent.action).thenReturn(Intent.ACTION_MAIN)

        val launchMethod = AppLaunchMethod.parse(safeIntent)

        Assert.assertEquals(AppLaunchMethod.LAUNCHER, launchMethod)
    }

    @Test
    fun homeShortcutIntent_parse_shortcutMode() {
        val safeIntent = mock(SafeIntent::class.java)
        Mockito.`when`(safeIntent.action).thenReturn(Intent.ACTION_VIEW)
        Mockito.`when`(safeIntent.getBooleanExtra(AppLaunchMethod.EXTRA_HOME_SCREEN_SHORTCUT, false)).thenReturn(true)

        val launchMethod = AppLaunchMethod.parse(safeIntent)

        Assert.assertEquals(AppLaunchMethod.HOME_SCREEN_SHORTCUT, launchMethod)
    }

    @Test
    fun textSelectionSearchIntent_parse_textSelectionMode() {
        val safeIntent = mock(SafeIntent::class.java)
        Mockito.`when`(safeIntent.action).thenReturn(Intent.ACTION_VIEW)
        Mockito.`when`(safeIntent.getBooleanExtra(AppLaunchMethod.EXTRA_TEXT_SELECTION, false)).thenReturn(true)

        val launchMethod = AppLaunchMethod.parse(safeIntent)

        Assert.assertEquals(AppLaunchMethod.TEXT_SELECTION_SEARCH, launchMethod)
    }

    @Test
    fun externalAppLaunchIntent_parse_externalAppMode() {
        val safeIntent = mock(SafeIntent::class.java)
        Mockito.`when`(safeIntent.action).thenReturn(Intent.ACTION_VIEW)

        val launchMethod = AppLaunchMethod.parse(safeIntent)

        Assert.assertEquals(AppLaunchMethod.EXTERNAL_APP, launchMethod)
    }

    @Test
    fun emptyIntent_parse_defaultLauncherMode() {
        val safeIntent = mock(SafeIntent::class.java)
        Mockito.`when`(safeIntent.action).thenReturn(null)

        val launchMethod = AppLaunchMethod.parse(safeIntent)

        Assert.assertEquals(AppLaunchMethod.UNKNOWN, launchMethod)
    }
}