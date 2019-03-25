package org.mozilla.focus.utils;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.activity.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mozilla.focus.utils.AppConfigWrapper.DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
import static org.mozilla.focus.utils.AppConfigWrapper.SURVEY_NOTIFICATION_POST_THRESHOLD;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG;

@RunWith(AndroidJUnit4.class)
public class AppConfigWrapperTest {

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