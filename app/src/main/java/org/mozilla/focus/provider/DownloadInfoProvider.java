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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.mozilla.focus.provider.DownloadContract.Download;

/**
 * Created by anlin on 17/08/2017.
 */

public class DownloadInfoProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(DownloadContract.AUTHORITY, DownloadContract.PATH, DownloadContract.CODE);
    }

    private DownloadInfoDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = DownloadInfoDbHelper.getsInstance(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case DownloadContract.CODE:

                queryBuilder.setTables(Download.TABLE_DOWNLOAD);
                break;
            default:
                throw new IllegalArgumentException("URI: " + uri);
        }

        SQLiteDatabase sqLiteDatabase = mDbHelper.getReadableDB();
        Cursor cursor = queryBuilder.query(sqLiteDatabase, projection, selection, selectionArgs, null, null, sortOrder, getLimitParam(uri.getQueryParameter("offset"), uri.getQueryParameter("limit")));
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        switch (sUriMatcher.match(uri)) {
            case DownloadContract.CODE:
                return Download.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDB();
        Long id;
        switch (sUriMatcher.match(uri)) {
            case DownloadContract.CODE:
                id = sqLiteDatabase.insert(Download.TABLE_DOWNLOAD, null, contentValues);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        Uri uriWithId;
        if (id > 0) {
            notifyChange();
            uriWithId = ContentUris.withAppendedId(uri, id);
        } else {
            uriWithId = null;
        }

        return uriWithId;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDB();
        int count;
        switch (sUriMatcher.match(uri)) {
            case DownloadContract.CODE:
                count = sqLiteDatabase.delete(Download.TABLE_DOWNLOAD, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase sqLiteDatabase = mDbHelper.getWritableDB();
        int count;
        switch (sUriMatcher.match(uri)) {
            case DownloadContract.CODE:
                count = sqLiteDatabase.update(Download.TABLE_DOWNLOAD, contentValues, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("URI: " + uri);
        }

        if (count > 0) {
            notifyChange();
        }

        return count;
    }

    private void notifyChange() {
        getContext().getContentResolver().notifyChange(Download.CONTENT_URI, null);
    }

    private String getLimitParam(String offset, String limit) {
        return (limit == null) ? null : (offset == null) ? limit : offset + "," + limit;
    }
}
