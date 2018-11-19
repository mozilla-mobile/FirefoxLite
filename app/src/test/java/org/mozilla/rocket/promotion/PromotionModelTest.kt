package org.mozilla.rocket.promotion

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings

class PromotionModelTest {

    @Test
    fun intentHasValidExtraShouldShouldRateAppDialog() {

        val safeIntent = mock(SafeIntent::class.java)
        val context = mock(Context::class.java)
        val eventHistory = mock(Settings.EventHistory::class.java)
        val newFeatureNotice = mock(NewFeatureNotice::class.java)

        `when`(safeIntent.getBooleanExtra(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false)).thenReturn(true)
        `when`(newFeatureNotice.shouldShowPrivacyPolicyUpdate()).thenReturn(false)

        val promotionModel = PromotionModel(context, eventHistory, newFeatureNotice, safeIntent)

        promotionModel.parseIntent(safeIntent)
        assertEquals(true, promotionModel.showRateAppDialogFromIntent)
    }
}
