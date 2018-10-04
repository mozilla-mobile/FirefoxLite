/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.provider.QueryHandler.AsyncDeleteListener;
import org.mozilla.focus.provider.QueryHandler.AsyncDeleteWrapper;
import org.mozilla.focus.provider.QueryHandler.AsyncInsertListener;
import org.mozilla.focus.provider.QueryHandler.AsyncQueryListener;
import org.mozilla.focus.provider.QueryHandler.AsyncUpdateListener;
import org.mozilla.focus.provider.ScreenshotContract.Screenshot;
import org.mozilla.focus.utils.IOUtils;
import org.mozilla.urlutils.UrlUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by hart on 16/08/2017.
 */

public class ScreenshotManager {

    private static final String TAG = "ScreenshotManager";

    private static final String CATEGORY_DEFAULT = "Others";
    private static final String CATEGORY_ERROR = "Error";

    private static volatile ScreenshotManager sInstance;

    HashMap<String, String> categories = new HashMap<>();

    private QueryHandler mQueryHandler;

    public static ScreenshotManager getInstance() {
        if (sInstance == null) {
            synchronized (ScreenshotManager.class) {
                if (sInstance == null) {
                    sInstance = new ScreenshotManager();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        mQueryHandler = new QueryHandler(context.getContentResolver());
    }

    public void insert(org.mozilla.focus.screenshot.model.Screenshot screenshot, AsyncInsertListener listener) {
        mQueryHandler.startInsert(QueryHandler.SCREENSHOT_TOKEN, listener, Screenshot.CONTENT_URI, QueryHandler.getContentValuesFromScreenshot(screenshot));
    }

    public void delete(long id, AsyncDeleteListener listener) {
        mQueryHandler.startDelete(QueryHandler.SCREENSHOT_TOKEN, new AsyncDeleteWrapper(id, listener), Screenshot.CONTENT_URI, Screenshot._ID + " = ?", new String[]{Long.toString(id)});
    }

    public void update(org.mozilla.focus.screenshot.model.Screenshot screenshot, AsyncUpdateListener listener) {
        mQueryHandler.startUpdate(QueryHandler.SCREENSHOT_TOKEN, listener, Screenshot.CONTENT_URI, QueryHandler.getContentValuesFromScreenshot(screenshot), Screenshot._ID + " = ?", new String[]{Long.toString(screenshot.getId())});
    }

    public void query(int offset, int limit, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SCREENSHOT_TOKEN, listener, Uri.parse(Screenshot.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit), null, null, null, Screenshot.TIMESTAMP + " DESC");
    }

    @WorkerThread
    private void lazyInitCategories(Context context) {
        try {
            if (categories.size() != 0) {
                return;
            }
            final JSONObject json;

            json = IOUtils.readAsset(context, "screenshots-mapping.json");

            final Iterator<String> iterator = json.keys();
            while (iterator.hasNext()) {
                final String category = iterator.next();
                final Object o = json.get(category);
                if (o instanceof JSONArray) {
                    JSONArray array = ((JSONArray) o);
                    for (int i = 0; i < array.length(); i++) {
                        final Object domain = array.get(i);
                        if (domain instanceof String) {
                            categories.put((String) domain, category);
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "ScreenshotManager init error: ", e);
        }
    }

    public String getCategory(Context context, String url) {

        lazyInitCategories(context);

        try {
            // if category is not ready, return empty string
            if (categories.size() == 0) {
                throw new IllegalStateException("Screenshot category is not ready!");
            }
            final String authority = UrlUtils.stripCommonSubdomains(new URL(url).getAuthority());
            for (Map.Entry<String, String> entry : categories.entrySet()) {
                if (authority.endsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return CATEGORY_DEFAULT;

        } catch (MalformedURLException e) {
            // if there's an exception, return error code
            return CATEGORY_ERROR;
        }
    }

    @VisibleForTesting
    public HashMap<String, String> getCategories(Context context) {

        lazyInitCategories(context);

        return categories;
    }
}
