package org.mozilla.focus.utils;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.activity.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.mozilla.focus.utils.AppConfigWrapper.DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
import static org.mozilla.focus.utils.AppConfigWrapper.SURVEY_NOTIFICATION_POST_THRESHOLD;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG;

@RunWith(AndroidJUnit4.class)
public class AppConfigWrapperTest {


    @Test
    public void getRateAppNotificationLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold(InstrumentationRegistry.getTargetContext());
        assertEquals(APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION, threshold);

    }

    @Test
    public void getShareDialogLaunchTimeThreshold() {
        final long extended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(InstrumentationRegistry.getTargetContext(), false);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG, extended);

        final long nonExtended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(InstrumentationRegistry.getTargetContext(), true);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG + APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION - APP_CREATE_THRESHOLD_FOR_RATE_DIALOG, nonExtended);
    }

    @Test
    public void getRateDialogLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateDialogLaunchTimeThreshold(InstrumentationRegistry.getTargetContext());
        assertEquals(APP_CREATE_THRESHOLD_FOR_RATE_DIALOG, threshold);

    }

    @Test
    public void getSurveyNotificationLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getSurveyNotificationLaunchTimeThreshold();
        assertEquals(SURVEY_NOTIFICATION_POST_THRESHOLD, threshold);
    }

    @Test
    public void getDriveDefaultBrowserFromMenuSettingThreshold() {
        final long threshold = AppConfigWrapper.getDriveDefaultBrowserFromMenuSettingThreshold();
        assertEquals(DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD, threshold);
    }
}