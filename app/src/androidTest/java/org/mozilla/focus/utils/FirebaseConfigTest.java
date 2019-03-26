package org.mozilla.focus.utils;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FirebaseConfigTest {

    @Test
    public void validateFirebaseSetting() {
        final long rateAppDialog = AppConfigWrapper.getRateDialogLaunchTimeThreshold();
        final long rateAppNotification = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold();
        final long shareDialogExtend = AppConfigWrapper.getShareDialogLaunchTimeThreshold(true);
        final long shareDialog = AppConfigWrapper.getShareDialogLaunchTimeThreshold(false);
        assertTrue(rateAppDialog < rateAppNotification);
        assertTrue(shareDialogExtend == rateAppNotification + shareDialog - rateAppDialog);
        assertTrue(rateAppDialog < shareDialog);
    }
}