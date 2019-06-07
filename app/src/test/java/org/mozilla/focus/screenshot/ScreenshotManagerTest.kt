package org.mozilla.focus.screenshot

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.FirebaseNoOpImp
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotManagerTest {

    @Test
    fun testCategories() {
        val sm = ScreenshotManager()
        val context = ApplicationProvider.getApplicationContext<Application>()

        FirebaseHelper.replaceContract(FirebaseNoOpImp())
        assert(sm.getCategory(context, "https://alipay.com/").equals("Banking"))
        assert(sm.getCategory(context, "https://m.alipay.com/").equals("Banking"))

        assert(sm.getCategory(context, "https://blogspot.com/").equals("Weblogs"))
        assert(sm.getCategory(context, "https://m.blogspot.com/").equals("Weblogs"))
    }
}
