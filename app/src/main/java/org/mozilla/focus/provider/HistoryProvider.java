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

import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;
import org.mozilla.focus.provider.HistoryDatabaseHelper.Tables;
import org.mozilla.focus.utils.ProviderUtils;

public class HistoryProvider extends ContentProvider {

    private static final int BROWSING_HISTORY = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(HistoryContract.AUTHORITY, "browsing_history", BROWSING_HISTORY);
    }

    private HistoryDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = HistoryDatabaseHelper.getsInstacne(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWriteableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case BROWSING_HISTORY:
                count = db.delete(Tables.BROWSING_HISTORY, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (count > 0) {
            notifyBrowsingHistoryChange();
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case BROWSING_HISTORY:
                return BrowsingHistory.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        final SQLiteDatabase db = mDbHelper.getWriteableDatabase();
        long id;
        switch (sUriMatcher.match(uri)) {
            case BROWSING_HISTORY:
                final ContentValues values = new ContentValues(initialValues);
                id = insertWithUrlUnique(db, values);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (id < 0) {
            return null;
        } else {
            notifyBrowsingHistoryChange();
            return ContentUris.withAppendedId(uri, id);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case BROWSING_HISTORY:
                qb.setTables(Tables.BROWSING_HISTORY);
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
        final SQLiteDatabase db = mDbHelper.getWriteableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case BROWSING_HISTORY:
                count = db.update(Tables.BROWSING_HISTORY, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (count > 0) {
            notifyBrowsingHistoryChange();
        }
        return count;
    }

    private long insertWithUrlUnique(SQLiteDatabase db, ContentValues values) {
        long id = -1;
        Cursor c = null;
        try {
            c = db.query(Tables.BROWSING_HISTORY, null, BrowsingHistory.URL + " = ?", new String[] {values.getAsString(BrowsingHistory.URL)}, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    id = c.getLong(c.getColumnIndex(BrowsingHistory._ID));
                    values.put(BrowsingHistory.VIEW_COUNT, c.getLong((c.getColumnIndex(BrowsingHistory.VIEW_COUNT))) + 1);
                    if (db.update(Tables.BROWSING_HISTORY, values, BrowsingHistory._ID + " = ?", new String[] {Long.toString(id)}) == 0) {
                        id = -1;
                    }
                } else {
                    id = db.insert(Tables.BROWSING_HISTORY, null, values);
                }
            }
            return id;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void notifyBrowsingHistoryChange() {
        getContext().getContentResolver().notifyChange(BrowsingHistory.CONTENT_URI, null);
    }
}
