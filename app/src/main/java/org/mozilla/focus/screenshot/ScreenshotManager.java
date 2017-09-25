/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot;

import android.content.Context;
import android.net.Uri;

import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.provider.QueryHandler.*;
import org.mozilla.focus.provider.ScreenshotContract.Screenshot;

/**
 * Created by hart on 16/08/2017.
 */

public class ScreenshotManager {

    private static ScreenshotManager sInstance;

    private QueryHandler mQueryHandler;

    public static ScreenshotManager getInstance() {
        if (sInstance == null) {
            sInstance = new ScreenshotManager();
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
        mQueryHandler.startDelete(QueryHandler.SCREENSHOT_TOKEN, new AsyncDeleteWrapper(id, listener), Screenshot.CONTENT_URI, Screenshot._ID + " = ?", new String[] {Long.toString(id)});
    }

    public void update(org.mozilla.focus.screenshot.model.Screenshot screenshot, AsyncUpdateListener listener) {
        mQueryHandler.startUpdate(QueryHandler.SCREENSHOT_TOKEN, listener, Screenshot.CONTENT_URI, QueryHandler.getContentValuesFromScreenshot(screenshot), Screenshot._ID + " = ?", new String[] {Long.toString(screenshot.getId())});
    }

    public void query(int offset, int limit, AsyncQueryListener listener) {
        mQueryHandler.startQuery(QueryHandler.SCREENSHOT_TOKEN, listener, Uri.parse(Screenshot.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit), null, null, null, Screenshot.TIMESTAMP + " DESC");
    }
}
