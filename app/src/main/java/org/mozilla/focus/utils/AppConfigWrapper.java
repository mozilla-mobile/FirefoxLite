/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.rocket.appupdate.InAppUpdateConfig;
import org.mozilla.rocket.appupdate.InAppUpdateIntro;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.MenuItemAdapter;

import java.util.ArrayList;
import java.util.List;

public class AppConfigWrapper {
    static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;
    static final boolean RC_KEY_ENABLE_SHOPPING_SEARCH_DEFAULT = true;


    /* Disabled since v1.0.4, keep related code in case we want to enable it again in the future */
    private static final boolean SURVEY_NOTIFICATION_ENABLED = false;
    static final int DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD = 2;

    public static long getRateAppNotificationLaunchTimeThreshold() {
        return FirebaseHelper.getFirebase().getRcLong(FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD);
    }

    public static long getShareDialogLaunchTimeThreshold(final boolean needExtend) {
        if (needExtend) {
            return FirebaseHelper.getFirebase().getRcLong(FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD) +
                    getRateAppNotificationLaunchTimeThreshold() -
                    getRateDialogLaunchTimeThreshold();
        }
        return FirebaseHelper.getFirebase().getRcLong(FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD);
    }

    public static long getRateDialogLaunchTimeThreshold() {
        return FirebaseHelper.getFirebase().getRcLong(FirebaseHelper.RATE_APP_DIALOG_THRESHOLD);
    }

    public static int getSurveyNotificationLaunchTimeThreshold() {
        return SURVEY_NOTIFICATION_POST_THRESHOLD;
    }

    public static int getDriveDefaultBrowserFromMenuSettingThreshold() {
        return DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
    }

    public static boolean isSurveyNotificationEnabled() {
        return SURVEY_NOTIFICATION_ENABLED;
    }

    public static String getRateAppDialogTitle() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE);
    }

    public static String getRateAppDialogContent() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT);
    }

    public static String getRateAppPositiveString() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.RATE_APP_DIALOG_TEXT_POSITIVE);
    }

    public static String getRateAppNegativeString() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.RATE_APP_DIALOG_TEXT_NEGATIVE);
    }

    public static String getScreenshotCategoryUrl() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.SCREENSHOT_CATEGORY_MANIFEST);
    }

    public static long getFirstLaunchWorkerTimer() {
        return FirebaseHelper.getFirebase().getRcLong(FirebaseHelper.FIRST_LAUNCH_TIMER_MINUTES);
    }

    public static String getFirstLaunchNotification() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.FIRST_LAUNCH_NOTIFICATION);
    }

    static String getShareAppDialogTitle() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_SHARE_APP_DIALOG_TITLE);
    }

    // Only this field supports prettify
    static String getShareAppDialogContent() {
        final String rcString = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_SHARE_APP_DIALOG_CONTENT);
        return FirebaseHelper.prettify(rcString);
    }

    static String getShareAppMessage() {
        return FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_SHARE_APP_DIALOG_MSG);
    }

    public static List<BottomBarItemAdapter.ItemData> getBottomBarItems() {
        List<BottomBarItemAdapter.ItemData> itemDataList = new ArrayList<>();
        String jsonString = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_BOTTOM_BAR_ITEMS_V2);
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject row = jsonArray.getJSONObject(i);
                int type = row.getInt("type");
                itemDataList.add(new BottomBarItemAdapter.ItemData(type));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return itemDataList;
    }

    public static List<MenuItemAdapter.ItemData> getMenuItems() {
        List<MenuItemAdapter.ItemData> itemDataList = new ArrayList<>();
        String jsonString = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_MENU_ITEMS);
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject row = jsonArray.getJSONObject(i);
                int type = row.getInt("type");
                itemDataList.add(new MenuItemAdapter.ItemData(type));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return itemDataList;
    }

    public static List<BottomBarItemAdapter.ItemData> getMenuBottomBarItems() {
        List<BottomBarItemAdapter.ItemData> itemDataList = new ArrayList<>();
        String jsonString = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_MENU_BOTTOM_BAR_ITEMS);
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject row = jsonArray.getJSONObject(i);
                int type = row.getInt("type");
                itemDataList.add(new BottomBarItemAdapter.ItemData(type));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return itemDataList;
    }

    public static List<BottomBarItemAdapter.ItemData> getPrivateBottomBarItems() {
        List<BottomBarItemAdapter.ItemData> itemDataList = new ArrayList<>();
        String jsonString = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_PRIVATE_BOTTOM_BAR_ITEMS);
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject row = jsonArray.getJSONObject(i);
                int type = row.getInt("type");
                itemDataList.add(new BottomBarItemAdapter.ItemData(type));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return itemDataList;
    }

    @Nullable
    public static InAppUpdateConfig getInAppUpdateConfig() {
        boolean showIntro = FirebaseHelper.getFirebase().getRcBoolean(FirebaseHelper.BOOL_IN_APP_UPDATE_SHOW_INTRO);
        String config = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_IN_APP_UPDATE_CONFIG);
        return convertToInAppUpdateConfig(config, showIntro);
    }

    @Nullable
    private static InAppUpdateIntro getInAppUpdateIntro(JSONObject obj) {
        try {
            JSONObject introObj = obj.getJSONObject("intro");
            return new InAppUpdateIntro(introObj.getString("title"),
                    introObj.getString("description"),
                    introObj.getString("positive"),
                    introObj.getString("negative"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static InAppUpdateConfig convertToInAppUpdateConfig(String config, boolean showIntro) {
        try {
            JSONObject obj = new JSONObject(config);
            InAppUpdateIntro intro = getInAppUpdateIntro(obj);
            return new InAppUpdateConfig(obj.getInt("targetVersion"),
                    obj.getBoolean("forceClose"),
                    showIntro,
                    intro);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
