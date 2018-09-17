package org.mozilla.focus.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScreenshotManagerTest {

    @Test
    fun testCategories() {
        val sm = ScreenshotManager()
        val context = RuntimeEnvironment.application
        assert(sm.getCategory(context, "https://alipay.com/").equals("Banking"))
        assert(sm.getCategory(context, "https://m.alipay.com/").equals("Banking"))

        assert(sm.getCategory(context, "https://blogspot.com/").equals("Weblogs"))
        assert(sm.getCategory(context, "https://m.blogspot.com/").equals("Weblogs"))

    }
}
