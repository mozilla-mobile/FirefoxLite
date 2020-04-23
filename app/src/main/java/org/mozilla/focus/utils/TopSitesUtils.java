/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.annotation.RawRes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.rocket.abtesting.LocalAbTesting;
import org.mozilla.rocket.home.topsites.data.TopSitesRepo;
import org.mozilla.rocket.home.topsites.domain.GetTopSitesAbTestingUseCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ylai on 2017/8/14.
 */

public class TopSitesUtils {

    public static final String TOP_SITE_ASSET_PREFIX = "file:///android_asset/topsites/icon/";

    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_VIEW_COUNT = "viewCount";
    public static final String KEY_LAST_VIEW_TIMESTAMP = "lastViewTimestamp";
    public static final String KEY_FAVICON = "favicon";

    /**
     * get default topsites data from assets and restore it to SharedPreferences
     *
     * @param context
     * @return default TopSites Json Array
     */
    public static JSONArray getDefaultSitesJsonArrayFromAssets(Context context) {
        JSONArray obj = null;
        try {
            // TODO: Remove after top site AB testing finished
            if (LocalAbTesting.INSTANCE.isExperimentEnabled(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) &&
                    LocalAbTesting.INSTANCE.checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) != null) {
                obj = new JSONArray(loadDefaultSitesFromAssets(context, R.raw.abtesting_topsites));
                String bucket = LocalAbTesting.INSTANCE.checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES);
                int fixedSiteCount = GetTopSitesAbTestingUseCase.Companion.getFixedSiteCount(bucket);
                int defaultPinCount = GetTopSitesAbTestingUseCase.Companion.getDefaultPinCount(bucket);
                for (int i = 0; i < fixedSiteCount + defaultPinCount; i++) {
                    obj.remove(0);
                }
            } else {
                obj = new JSONArray(loadDefaultSitesFromAssets(context, R.raw.topsites));
            }

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

    public static void clearTopSiteData(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(TopSitesRepo.TOP_SITES_PREF)
                .apply();
    }

    public static String loadDefaultSitesFromAssets(Context context, @RawRes int resId) {
        String json = "[]";
        try (final InputStream is = context.getResources().openRawResource(resId)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }

    public static void saveDefaultSites(Context context, JSONArray obj) {
        if (context == null) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(TopSitesRepo.TOP_SITES_PREF, obj.toString())
                .apply();
    }

    public static Site paresSite(JSONObject jsonObject) throws JSONException {
        final long id = jsonObject.getLong(KEY_ID);
        final String title = jsonObject.getString(KEY_TITLE);
        final String url = jsonObject.getString(KEY_URL);
        final long viewCount = jsonObject.getLong(KEY_VIEW_COUNT);
        long lastViewed = 0;
        if (jsonObject.has(KEY_LAST_VIEW_TIMESTAMP)) {
            lastViewed = jsonObject.getLong(KEY_LAST_VIEW_TIMESTAMP);
        }
        final String faviconUri = TOP_SITE_ASSET_PREFIX + jsonObject.getString(KEY_FAVICON);
        return new Site(id, title, url, viewCount, lastViewed, faviconUri);
    }
}
