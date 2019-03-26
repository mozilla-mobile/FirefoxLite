package org.mozilla.focus.utils

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mozilla.focus.utils.FirebaseHelper.RATE_APP_DIALOG_THRESHOLD
import org.mozilla.focus.utils.FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD
import org.mozilla.focus.utils.FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD

/**
 * Make sure default value will be used.
 */
class AppConfigWrapperTest {

    @Test
    fun `customize default value`() {

        val rateDialog = 3
        val rateNotification = 4
        val shareDialog = 5

        val map = HashMap<String, Any>().apply {
            this[RATE_APP_DIALOG_THRESHOLD] = rateDialog
            this[RATE_APP_NOTIFICATION_THRESHOLD] = rateNotification
            this[SHARE_APP_DIALOG_THRESHOLD] = shareDialog
        }

        FirebaseHelper.init(mock(Context::class.java), false, FirebaseNoOpImp(map))

        assertEquals(rateDialog, AppConfigWrapper.getRateDialogLaunchTimeThreshold().toInt())

        assertEquals(
                rateNotification,
                AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold().toInt()
        )

        assertEquals(
                shareDialog,
                AppConfigWrapper.getShareDialogLaunchTimeThreshold(false).toInt()
        )

        assertEquals(
                shareDialog + rateNotification - rateDialog,
                AppConfigWrapper.getShareDialogLaunchTimeThreshold(true).toInt()
        )
    }
}