package org.mozilla.focus.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.FirebaseNoOpImp
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScreenshotManagerTest {

    @Test
    fun testCategories() {
        val sm = ScreenshotManager()
        val context = RuntimeEnvironment.application

        FirebaseHelper.init(context, false, FirebaseNoOpImp())

        assert(sm.getCategory(context, "https://alipay.com/").equals("Banking"))
        assert(sm.getCategory(context, "https://m.alipay.com/").equals("Banking"))

        assert(sm.getCategory(context, "https://blogspot.com/").equals("Weblogs"))
        assert(sm.getCategory(context, "https://m.blogspot.com/").equals("Weblogs"))
    }
}
