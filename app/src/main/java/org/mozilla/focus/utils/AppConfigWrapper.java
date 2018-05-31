/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;

public class AppConfigWrapper {
    private static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;

    /* Disabled since v1.0.4, keep related code in case we want to enable it again in the future */
    private static final boolean SURVEY_NOTIFICATION_ENABLED = false;
    private static final int DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD = 2;

    public static long getRateAppNotificationLaunchTimeThreshold(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD);
    }

    public static long getShareDialogLaunchTimeThreshold(Context context, final boolean needExtend) {
        if (needExtend) {
            return FirebaseHelper.getRcLong(context, FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD) +
                    getRateAppNotificationLaunchTimeThreshold(context) -
                    getRateDialogLaunchTimeThreshold(context);
        }
        return FirebaseHelper.getRcLong(context, FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD);
    }

    public static long getRateDialogLaunchTimeThreshold(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.RATE_APP_DIALOG_THRESHOLD);
    }

    public static int getSurveyNotificationLaunchTimeThreshold() {
        return SURVEY_NOTIFICATION_POST_THRESHOLD;
    }

    public static int getDriveDefaultBrowserFromMenuSettingThreshold() {
        return DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
    }

    public static boolean getMyshotUnreadEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_MY_SHOT_UNREAD);
    }

    public static boolean isSurveyNotificationEnabled() {
        return SURVEY_NOTIFICATION_ENABLED;
    }
}
