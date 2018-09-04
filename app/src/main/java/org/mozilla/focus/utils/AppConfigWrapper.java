/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;

public class AppConfigWrapper {
    static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;
    static final boolean PRIVATE_MODE_ENABLED_DEFAULT = true;

    /* Disabled since v1.0.4, keep related code in case we want to enable it again in the future */
    private static final boolean SURVEY_NOTIFICATION_ENABLED = false;
    static final int DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD = 2;

    private static final String CUSTOM_TOP_SITES_URL_KEY = "CUSTOM_TOP_SITES_URL_KEY";

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

    public static boolean isPrivateModeEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_PRIVATE_MODE);
    }

    public static boolean getMyshotUnreadEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_MY_SHOT_UNREAD);
    }

    public static boolean isSurveyNotificationEnabled() {
        return SURVEY_NOTIFICATION_ENABLED;
    }

    public static String getRateAppDialogTitle(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE);
    }

    public static String getRateAppDialogContent(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT);
    }

    public static String getBannerRootConfig(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.BANNER_MANIFEST);
    }

    public static String getCustomTopSitesUri(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(CUSTOM_TOP_SITES_URL_KEY, null);
    }

    public static void setCustomTopSitesUri(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(CUSTOM_TOP_SITES_URL_KEY, value).apply();
    }
}
