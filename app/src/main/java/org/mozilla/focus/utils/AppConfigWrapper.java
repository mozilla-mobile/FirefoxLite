/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

public class AppConfigWrapper {
    private static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;

    /* Disabled since v1.0.4, keep related code in case we want to enable it again in the future */
    private static final boolean SURVEY_NOTIFICATION_ENABLED = false;

    public static int getRateNotificationLaunchTimeThreshold() {
        return DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION;
    }

    public static int getShareDialogLaunchTimeThreshold(boolean needExtend) {
        // Dismiss in Love Firefox is clicked, need to wait till notification to fire share dialog
        if (needExtend) {
            return DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG +
                    DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION -
                    DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG;
        }
        return DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG;
    }

    public static int getRateDialogLaunchTimeThreshold() {
        return DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG;
    }

    public static int getSurveyNotificationLaunchTimeThreshold() {
        return SURVEY_NOTIFICATION_POST_THRESHOLD;
    }

    public static boolean isSurveyNotificationEnabled() {
        return SURVEY_NOTIFICATION_ENABLED;
    }
}
