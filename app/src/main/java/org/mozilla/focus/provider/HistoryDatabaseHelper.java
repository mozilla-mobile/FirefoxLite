package org.mozilla.focus.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static org.mozilla.focus.provider.HistoryContract.BrowsingHistory;

/**
 * Created by hart on 03/08/2017.
 */

public class HistoryDatabaseHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "history.db";

    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    private static final String DROP_TRIGGER_IF_EXISTS = "DROP TRIGGER IF EXISTS ";
    private static final String CREATE_TRIGGER_IF_NOT_EXISTS = "CREATE TRIGGER IF NOT EXISTS ";
    private static final int HISTORY_LIMIT = 2000;

    private static HistoryDatabaseHelper sInstacne;

    private final OpenHelper mOpenHelper;

    public interface Tables {
        String BROWSING_HISTORY = "browsing_history";
    }

    private static final class OpenHelper extends SQLiteOpenHelper {

        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_IF_EXISTS + Tables.BROWSING_HISTORY);
            db.execSQL(CREATE_TABLE_IF_NOT_EXISTS + Tables.BROWSING_HISTORY + " (" +
                    BrowsingHistory._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    BrowsingHistory.TITLE + " TEXT," +
                    BrowsingHistory.URL + " TEXT NOT NULL," +
                    BrowsingHistory.VIEW_COUNT + " INTEGER NOT NULL DEFAULT 1," +
                    BrowsingHistory.LAST_VIEW_TIMESTAMP + " INTEGER NOT NULL," +
                    BrowsingHistory.FAV_ICON + " BLOB" +
                    ");");

            db.execSQL(DROP_TRIGGER_IF_EXISTS + Tables.BROWSING_HISTORY + "_inserted;");
            db.execSQL(CREATE_TRIGGER_IF_NOT_EXISTS + Tables.BROWSING_HISTORY + "_inserted " +
                    "   AFTER INSERT ON " + Tables.BROWSING_HISTORY +
                    " WHEN (SELECT count() FROM " + Tables.BROWSING_HISTORY + ") > " + HISTORY_LIMIT +
                    " BEGIN " +
                    "   DELETE FROM " + Tables.BROWSING_HISTORY +
                    "     WHERE " + BrowsingHistory._ID + " = " +
                          "(SELECT " + BrowsingHistory._ID +
                          " FROM " + Tables.BROWSING_HISTORY +
                          " ORDER BY " + BrowsingHistory.LAST_VIEW_TIMESTAMP +
                          " LIMIT 1);" +
                    " END");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private HistoryDatabaseHelper(Context context) {
        mOpenHelper = new OpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized HistoryDatabaseHelper getsInstacne(Context context) {
        if (sInstacne == null) {
            sInstacne = new HistoryDatabaseHelper(context);
        }
        return sInstacne;
    }

    public SQLiteDatabase getReadableDatabase() {
        return mOpenHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWriteableDatabase() {
        return mOpenHelper.getWritableDatabase();
    }
}
