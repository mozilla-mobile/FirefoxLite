/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.screenshot.model.Screenshot;
import org.mozilla.focus.utils.DimenUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hart on 16/08/2017.
 */

public class QueryHandler extends AsyncQueryHandler {

    public static final int SITE_TOKEN = 1;
    public static final int SCREENSHOT_TOKEN = 2;
    public static final long LONG_NO_VALUE = 0;
    public static final Object OBJECT_NO_VALUE = null;
    private Handler mWorkerHandler;

    public static final class AsyncDeleteWrapper {

        public long id;
        public AsyncDeleteListener listener;

        public AsyncDeleteWrapper(long id, AsyncDeleteListener listener) {
            this.id = id;
            this.listener = listener;
        }
    }

    public interface AsyncInsertListener {
        void onInsertComplete(long id);
    }

    public interface AsyncDeleteListener {
        void onDeleteComplete(int result, long id);
    }

    public interface AsyncUpdateListener {
        void onUpdateComplete(int result);
    }

    public interface AsyncQueryListener {
        void onQueryComplete(List result);
    }

    public QueryHandler(ContentResolver resolver) {
        super(resolver);
    }

    @Override
    protected Handler createHandler(Looper looper) {
        mWorkerHandler = super.createHandler(looper);
        return mWorkerHandler;
    }

    public void postWorker(Runnable r) {
        mWorkerHandler.post(r);
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        switch (token) {
            case SITE_TOKEN:
            case SCREENSHOT_TOKEN:
                if (cookie != null) {
                    final long id = uri == null ? -1 : Long.parseLong(uri.getLastPathSegment());
                    ((AsyncInsertListener) cookie).onInsertComplete(id);
                }
                break;
            default:
                // do nothing
        }
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        switch (token) {
            case SITE_TOKEN:
            case SCREENSHOT_TOKEN:
                if (cookie != null) {
                    AsyncDeleteWrapper wrapper = ((AsyncDeleteWrapper) cookie);
                    if (wrapper.listener != null) {
                        wrapper.listener.onDeleteComplete(result, wrapper.id);
                    }
                }
                break;
            default:
                // do nothing
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        switch (token) {
            case SITE_TOKEN:
            case SCREENSHOT_TOKEN:
                if (cookie != null) {
                    ((AsyncUpdateListener) cookie).onUpdateComplete(result);
                }
                break;
            default:
                // do nothing
        }
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        switch (token) {
            case SITE_TOKEN:
                if (cookie != null) {
                    List sites = new ArrayList();
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            sites.add(cursorToSite(cursor));
                        }
                        cursor.close();
                    }
                    ((AsyncQueryListener) cookie).onQueryComplete(sites);
                }
                break;
            case SCREENSHOT_TOKEN:
                if (cookie != null) {
                    List screenshots = new ArrayList();
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            screenshots.add(cursorToScreenshot(cursor));
                        }
                        cursor.close();
                    }
                    ((AsyncQueryListener) cookie).onQueryComplete(screenshots);
                }
                break;
            default:
                // do nothing
        }
    }

    public static ContentValues getContentValuesFromSite(Site site) {
        ContentValues values = new ContentValues();
        if (site.getTitle() != OBJECT_NO_VALUE) {
            values.put(HistoryContract.BrowsingHistory.TITLE, site.getTitle());
        }
        values.put(HistoryContract.BrowsingHistory.URL, site.getUrl());
        if (site.getViewCount() != LONG_NO_VALUE) {
            values.put(HistoryContract.BrowsingHistory.VIEW_COUNT, site.getViewCount());
        }
        if (site.getLastViewTimestamp() != LONG_NO_VALUE) {
            values.put(HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP, site.getLastViewTimestamp());
        }
        if (site.getFavIconUri() != OBJECT_NO_VALUE) {
            values.put(HistoryContract.BrowsingHistory.FAV_ICON_URI, site.getFavIconUri());
        }
        return values;
    }

    public static ContentValues getContentValuesFromScreenshot(Screenshot screenshot) {
        ContentValues values = new ContentValues();
        if (screenshot.getTitle() != OBJECT_NO_VALUE) {
            values.put(ScreenshotContract.Screenshot.TITLE, screenshot.getTitle());
        }
        if (screenshot.getUrl() != OBJECT_NO_VALUE) {
            values.put(ScreenshotContract.Screenshot.URL, screenshot.getUrl());
        }
        if (screenshot.getTimestamp() != LONG_NO_VALUE) {
            values.put(ScreenshotContract.Screenshot.TIMESTAMP, screenshot.getTimestamp());
        }
        if (screenshot.getImageUri() != OBJECT_NO_VALUE) {
            values.put(ScreenshotContract.Screenshot.IMAGE_URI, screenshot.getImageUri());
        }
        return values;
    }

    private static Site cursorToSite(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(HistoryContract.BrowsingHistory._ID));
        final String title = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.TITLE));
        final String url = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.URL));
        final long viewCount = cursor.getLong(cursor.getColumnIndex(HistoryContract.BrowsingHistory.VIEW_COUNT));
        final long lastViewTimsTamp = cursor.getLong(cursor.getColumnIndex(HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP));
        final String faviconUri = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.FAV_ICON_URI));
        return new Site(id, title, url, viewCount, lastViewTimsTamp, faviconUri);
    }

    private static Screenshot cursorToScreenshot(Cursor cursor) {
        Screenshot screenshot = new Screenshot();
        screenshot.setId(cursor.getLong(cursor.getColumnIndex(ScreenshotContract.Screenshot._ID)));
        screenshot.setTitle(cursor.getString(cursor.getColumnIndex(ScreenshotContract.Screenshot.TITLE)));
        screenshot.setUrl(cursor.getString(cursor.getColumnIndex(ScreenshotContract.Screenshot.URL)));
        screenshot.setTimestamp(cursor.getLong(cursor.getColumnIndex(ScreenshotContract.Screenshot.TIMESTAMP)));
        screenshot.setImageUri(cursor.getString(cursor.getColumnIndex(ScreenshotContract.Screenshot.IMAGE_URI)));
        return screenshot;
    }

    private static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, DimenUtils.PNG_QUALITY_DONT_CARE, stream);
        return stream.toByteArray();
    }

    private static Bitmap bytesToBitmap(byte[] data) {
        return data == null ? null : BitmapFactory.decodeByteArray(data, 0, data.length);
    }
}
