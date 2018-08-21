/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQueryBuilder;
import android.arch.persistence.room.OnConflictStrategy;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;
import org.mozilla.focus.provider.HistoryDatabaseHelper.Tables;
import org.mozilla.focus.utils.ProviderUtils;
import org.mozilla.rocket.persistance.History.HistoryDatabase;

public class HistoryProvider extends ContentProvider {

    private static final int BROWSING_HISTORY = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(HistoryContract.AUTHORITY, "browsing_history", BROWSING_HISTORY);
    }

    private SupportSQLiteOpenHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = HistoryDatabase.getInstance(getContext()).getOpenHelper();
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
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
        final SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
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
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (sUriMatcher.match(uri) != BROWSING_HISTORY) {
            throw new IllegalArgumentException("URI: " + uri);
        }

        final SupportSQLiteDatabase db = mDbHelper.getReadableDatabase();
        SupportSQLiteQuery query = SupportSQLiteQueryBuilder.builder(Tables.BROWSING_HISTORY)
                .columns(projection)
                .selection(selection, selectionArgs)
                .groupBy(null)
                .having(null)
                .orderBy(sortOrder)
                .limit(ProviderUtils.getLimitParam(uri.getQueryParameter("offset"), uri.getQueryParameter("limit")))
                .create();

        return db.query(query);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (sUriMatcher.match(uri) != BROWSING_HISTORY) {
            throw new UnsupportedOperationException("URI: " + uri);
        }
        final SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        count = db.update(Tables.BROWSING_HISTORY, OnConflictStrategy.ROLLBACK, values, selection, selectionArgs);

        if (count > 0) {
            notifyBrowsingHistoryChange();
        }
        return count;
    }

    private long insertWithUrlUnique(SupportSQLiteDatabase db, ContentValues values) {
        long id = -1;
        Cursor c = null;
        try {
            SupportSQLiteQuery query = SupportSQLiteQueryBuilder.builder(Tables.BROWSING_HISTORY)
                    .columns(null)
                    .selection(BrowsingHistory.URL + " = ?", new String[]{values.getAsString(BrowsingHistory.URL)})
                    .groupBy(null)
                    .having(null)
                    .orderBy(null)
                    .create();
            c = db.query(query);
            if (c != null) {
                if (c.moveToFirst()) {
                    id = c.getLong(c.getColumnIndex(BrowsingHistory._ID));
                    values.put(BrowsingHistory.VIEW_COUNT, c.getLong((c.getColumnIndex(BrowsingHistory.VIEW_COUNT))) + 1);
                    if (db.update(Tables.BROWSING_HISTORY, OnConflictStrategy.ROLLBACK, values, BrowsingHistory._ID + " = ?", new String[]{Long.toString(id)}) == 0) {
                        id = -1;
                    }
                } else {
                    values.put(BrowsingHistory.VIEW_COUNT, 1);
                    id = db.insert(Tables.BROWSING_HISTORY, OnConflictStrategy.ROLLBACK, values);
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
