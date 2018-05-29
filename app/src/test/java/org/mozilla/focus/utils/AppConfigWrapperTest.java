package org.mozilla.focus.utils;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mozilla.focus.utils.AppConfigWrapper.DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
import static org.mozilla.focus.utils.AppConfigWrapper.SURVEY_NOTIFICATION_POST_THRESHOLD;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION;
import static org.mozilla.focus.utils.DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG;
import static org.mozilla.focus.utils.FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD;

public class AppConfigWrapperTest {

    @Mock
    Context context;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void warmUp() {
        when(context.getApplicationContext()).thenReturn(context);
        FirebaseHelper.init(context, false);

    }

    @Test
    public void getRateAppNotificationLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold(null);
        assertEquals(APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION, threshold);

    }

    @Test
    public void getShareDialogLaunchTimeThreshold() {
        final long extended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(null, false);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG, extended);

        final long nonExtended = AppConfigWrapper.getShareDialogLaunchTimeThreshold(null, true);
        assertEquals(APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG + APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION - APP_CREATE_THRESHOLD_FOR_RATE_DIALOG, nonExtended);
    }

    @Test
    public void getRateDialogLaunchTimeThreshold() {
        final long threshold = AppConfigWrapper.getRateDialogLaunchTimeThreshold(null);
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