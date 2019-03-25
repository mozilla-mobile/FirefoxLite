package org.mozilla.focus.utils;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mozilla.focus.utils.AppConfigWrapper.DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
import static org.mozilla.focus.utils.AppConfigWrapper.SURVEY_NOTIFICATION_POST_THRESHOLD;
import static org.mozilla.focus.utils.DialogUtils.*;

/**
 * Make sure default value will be used.
 */
@RunWith(RobolectricTestRunner.class)
public class AppConfigWrapperDefaultValueTest {

    @Before
    public void setup() {

        FirebaseHelper.init(mock(Context.class), false, new FirebaseNoOpImp());

    }


    @Test
    public void getRateAppNotificationLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold();
        assertEquals(APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION, threshold);
    }

    @Test
    public void getShareDialogLaunchTimeThreshold() {
        final long extended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(false);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG, extended);

        final long nonExtended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(true);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG + APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION - APP_CREATE_THRESHOLD_FOR_RATE_DIALOG, nonExtended);
    }

    @Test
    public void getRateDialogLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateDialogLaunchTimeThreshold();
        final boolean condition = APP_CREATE_THRESHOLD_FOR_RATE_DIALOG == threshold;
        assertTrue(condition);

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