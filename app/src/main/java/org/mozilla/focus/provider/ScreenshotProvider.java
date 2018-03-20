/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import org.mozilla.focus.provider.ScreenshotContract.Screenshot;
import org.mozilla.focus.provider.ScreenshotDatabaseHelper.Tables;
import org.mozilla.focus.utils.ProviderUtils;

/**
 * Created by hart on 15/08/2017.
 */

public class ScreenshotProvider extends ContentProvider {

    private static final int SCREENSHOT = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ScreenshotContract.AUTHORITY, "screenshot", SCREENSHOT);
    }

    private ScreenshotDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = ScreenshotDatabaseHelper.getsInstacne(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case SCREENSHOT:
                count = db.delete(Tables.SCREENSHOT, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case SCREENSHOT:
                return Screenshot.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id;
        switch (sUriMatcher.match(uri)) {
            case SCREENSHOT:
                final ContentValues values = new ContentValues(initialValues);
                id = db.insert(Tables.SCREENSHOT, null, values);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (id < 0) {
            return null;
        } else {
            notifyScreenshotChange();
            return ContentUris.withAppendedId(uri, id);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case SCREENSHOT:
                qb.setTables(Tables.SCREENSHOT);
                break;
            default:
                throw new IllegalArgumentException("URI: " + uri);
        }

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, ProviderUtils.getLimitParam(uri.getQueryParameter("offset"), uri.getQueryParameter("limit")));

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case SCREENSHOT:
                count = db.update(Tables.SCREENSHOT, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        return count;
    }

    private void notifyScreenshotChange() {
        getContext().getContentResolver().notifyChange(ScreenshotContract.Screenshot.CONTENT_URI, null);
    }
}
