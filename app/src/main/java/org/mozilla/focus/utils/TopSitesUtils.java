/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.home.HomeFragment;

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
     * @return default TopSites Json Array
     */
    public static JSONArray getDefaultSitesJsonArrayFromAssets(Context context) {
        JSONArray obj = null;
        try {
            obj = new JSONArray(loadDefaultSitesFromAssets(context));
            long lastViewTimestampSystem = System.currentTimeMillis();
            for (int i = 0; i < obj.length(); i++) {
                ((JSONObject) obj.get(i)).put("lastViewTimestamp", lastViewTimestampSystem);
            }
            saveDefaultSites(context, obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
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
                    final String faviconUri = TOP_SITE_ASSET_PREFIX + json_site.getString("favicon");
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
}
