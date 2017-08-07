package org.mozilla.focus.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.mozilla.focus.provider.HistoryContract.BrowsingHistory;
import org.mozilla.focus.provider.HistoryDatabaseHelper.Tables;

public class HistoryProvider extends ContentProvider {

    private static final int BROWSING_HISTORY = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(HistoryContract.AUTHORITY, "browsing_history", BROWSING_HISTORY);
    }

    private HistoryDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mDbHelper = HistoryDatabaseHelper.getsInstacne(context);
        return true;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
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
        long id = 0;
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
            return ContentUris.withAppendedId(uri, id);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
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
}
