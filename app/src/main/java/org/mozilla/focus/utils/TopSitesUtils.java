/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.rocket.distribution.LoadDistributionConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ylai on 2017/8/14.
 */

public class TopSitesUtils {

    private static final String TOP_SITE_ASSET_PREFIX = "file:///android_asset/topsites/icon/";

    /**
     * get default topsites data from assets and restore it to SharedPreferences
     *
     * @param context
     */
    private static void getDefaultSitesJsonArrayFromAssets(Context context, Handler handler, int messageId) {
        try {
            JSONArray obj = new JSONArray(loadDefaultSitesFromAssets(context));
            addLastViewTimeStamp(obj);
            saveDefaultSites(context, obj);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (handler != null) {
                notifyHomeFragment(handler, messageId);
            }
        }
    }

    public static void addLastViewTimeStamp(JSONArray jsonArray) {
        try {
            long lastViewTimestampSystem = System.currentTimeMillis();
            for (int i = 0; i < jsonArray.length(); i++) {
                ((JSONObject) jsonArray.get(i)).put("lastViewTimestamp", lastViewTimestampSystem);
            }
        } catch (JSONException ignored) {
            // Never happens
            ignored.printStackTrace();
        }
    }

    private static void getDefaultSitesJsonArrayFromUri(Context context, Handler handler, int messageId) {
        Intent startServiceIntent = new Intent(context, LoadDistributionConfigService.class);
        if (handler != null) {
            Messenger messengerIncoming = new Messenger(handler);
            startServiceIntent.putExtra(LoadDistributionConfigService.MESSENGER_INTENT_KEY, messengerIncoming);
            startServiceIntent.putExtra(LoadDistributionConfigService.MESSAGE_ID_INTENT_KEY, messageId);
        }
        context.startService(startServiceIntent);
    }

    // Called if we don't have the messenger and will be notified in another way.
    public static void initDefaultTopSites(Context context) {
        initDefaultTopSites(context, null, -1);
    }

    public static void initDefaultTopSites(Context context, Handler handler, int messageId) {
        if (AppConfigWrapper.getCustomTopSitesUri(context) == null) {
            getDefaultSitesJsonArrayFromAssets(context, handler, messageId);
        } else {
            getDefaultSitesJsonArrayFromUri(context, handler, messageId);
        }
    }

    private static void notifyHomeFragment(Handler handler, int messageId) {
        Message message = handler.obtainMessage(messageId);
        handler.dispatchMessage(message);
    }

    private static String loadDefaultSitesFromAssets(Context context) {
        String json = "[]";
        try {
            InputStream is = context.getResources().openRawResource(R.raw.topsites);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            return json;
        }
    }

    public static void saveDefaultSites(Context context, JSONArray obj) {
        if (context == null) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(HomeFragment.TOPSITES_PREF, obj.toString())
                .apply();
    }

    public static List<Site> paresJsonToList(JSONArray jsonArray) {
        List<Site> defaultSites = new ArrayList<>();
        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json_site = (JSONObject) jsonArray.get(i);
                    final long id = json_site.getLong("id");
                    final String title = json_site.getString("title");
                    final String url = json_site.getString("url");
                    final long viewCount = json_site.getLong("viewCount");
                    final long lastViewed = json_site.getLong("lastViewTimestamp");
                    final String faviconUriOrName = json_site.getString("favicon");
                    final String faviconUri;
                    if (Uri.parse(faviconUriOrName).getScheme() == null) {
                        faviconUri = TOP_SITE_ASSET_PREFIX + faviconUriOrName;
                    } else {
                        faviconUri = faviconUriOrName;
                    }
                    Site site = new Site(id, title, url, viewCount, lastViewed, faviconUri);
                    defaultSites.add(site);
                }
            }
            return defaultSites;
        } catch (JSONException e) {
            e.printStackTrace();
            return defaultSites;
        }
    }

    // For future use
    public static void onDistributionLoaded(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (url.equals(AppConfigWrapper.getCustomTopSitesUri(context))) {
            return;
        }
        AppConfigWrapper.setCustomTopSitesUri(context, url);
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(HomeFragment.TOPSITES_PREF).apply();
    }
}
