package org.mozilla.focus.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScreenshotManagerTest {

    @Test
    fun testInit() {
        val sm = ScreenshotManager()
        sm.initScreenShotCateogry(RuntimeEnvironment.application)
        assert(sm.categories.size > 0)
    }

    @Test
    fun `Telemetry with category should work`(){
        val sm = ScreenshotManager()
        sm.initScreenShotCateogry(RuntimeEnvironment.application)
        // expect no exception
        sm.categories.values.forEach {
            TelemetryWrapper.openCaptureLink(it)
        }
    }
}
