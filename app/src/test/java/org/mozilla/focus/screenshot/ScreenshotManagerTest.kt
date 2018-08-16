package org.mozilla.focus.screenshot

import org.junit.Test
import org.junit.runner.RunWith
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
}
