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
        sm.lazyInitCategories(RuntimeEnvironment.application)
        assert(sm.categories.size > 0)
    }

    @Test
    fun testCategories() {
        val sm = ScreenshotManager()
        sm.initScreenShotCateogry(RuntimeEnvironment.application)
        assert(sm.getCategory("https://alipay.com/").equals("Banking"))
        assert(sm.getCategory("https://m.alipay.com/").equals("Banking"))

        assert(sm.getCategory("https://blogspot.com/").equals("Weblogs"))
        assert(sm.getCategory("https://m.blogspot.com/").equals("Weblogs"))

    }
}
