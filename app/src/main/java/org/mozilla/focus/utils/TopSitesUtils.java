/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

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

    public static final String TOP_SITE_ASSET_PREFIX = "file:///android_asset/topsites/icon/";

    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_VIEW_COUNT = "viewCount";
    private static final String KEY_LAST_VIEW_TIMESTAMP = "lastViewTimestamp";
    private static final String KEY_FAVICON = "favicon";

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

    public static List<Site> paresJsonToList(Context context, JSONArray jsonArray) {
        List<Site> defaultSites = new ArrayList<>();
        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json_site = (JSONObject) jsonArray.get(i);
                    final long id = json_site.getLong(KEY_ID);
                    final String title = json_site.getString(KEY_TITLE);
                    final String url = json_site.getString(KEY_URL);
                    final long viewCount = json_site.getLong(KEY_VIEW_COUNT);
                    final long lastViewed = json_site.getLong(KEY_LAST_VIEW_TIMESTAMP);
                    final String faviconUri = TOP_SITE_ASSET_PREFIX + json_site.getString(KEY_FAVICON);
                    Site site = new Site(id, title, url, viewCount, lastViewed, faviconUri);
                    site.setDefault(true);
                    defaultSites.add(site);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return defaultSites;
        }
    }

    public static JSONArray sitesToJson(List<Site> sites) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < sites.size(); ++i) {
            Site site = sites.get(i);
            JSONObject jsonSite = siteToJson(site);
            if (jsonSite != null) {
                array.put(jsonSite);
            }
        }
        return array;
    }

    public static List<Site> jsonToSites(String jsonData) {
        List<Site> sites = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(jsonData);
            for (int i = 0; i < array.length(); ++i) {
                JSONObject obj = array.getJSONObject(i);
                sites.add(new Site(obj.getLong(KEY_ID),
                        obj.getString(KEY_TITLE),
                        obj.getString(KEY_URL),
                        obj.getLong(KEY_VIEW_COUNT),
                        obj.getLong(KEY_LAST_VIEW_TIMESTAMP),
                        obj.getString(KEY_FAVICON)));
            }

        } catch (JSONException ignored) {
        }
        return sites;
    }

    @Nullable
    private static JSONObject siteToJson(Site site) {
        try {
            JSONObject node = new JSONObject();
            node.put(KEY_ID, site.getId());
            node.put(KEY_URL, site.getUrl());
            node.put(KEY_TITLE, site.getTitle());
            node.put(KEY_FAVICON, site.getFavIconUri());
            node.put(KEY_LAST_VIEW_TIMESTAMP, site.getLastViewTimestamp());
            node.put(KEY_VIEW_COUNT, site.getViewCount());
            return node;
        } catch (JSONException e) {
            return null;
        }
    }
}
