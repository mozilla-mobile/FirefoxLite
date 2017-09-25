/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.mozilla.focus.provider.ScreenshotContract.Screenshot;

/**
 * Created by hart on 15/08/2017.
 */

public class ScreenshotDatabaseHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "screenshot.db";

    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    private static final String DROP_TRIGGER_IF_EXISTS = "DROP TRIGGER IF EXISTS ";
    private static final String CREATE_TRIGGER_IF_NOT_EXISTS = "CREATE TRIGGER IF NOT EXISTS ";
    private static final int SCREENSHOT_LIMIT = 2000;

    private static ScreenshotDatabaseHelper sInstacne;

    private final OpenHelper mOpenHelper;

    public interface Tables {
        String SCREENSHOT = "screenshot";
    }

    private static final class OpenHelper extends SQLiteOpenHelper {

        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_IF_EXISTS + Tables.SCREENSHOT);
            db.execSQL(CREATE_TABLE_IF_NOT_EXISTS + Tables.SCREENSHOT + " (" +
                    Screenshot._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Screenshot.TITLE + " TEXT," +
                    Screenshot.URL + " TEXT NOT NULL," +
                    Screenshot.TIMESTAMP + " INTEGER NOT NULL," +
                    Screenshot.IMAGE_URI + " TEXT NOT NULL" +
                    ");");

            db.execSQL(DROP_TRIGGER_IF_EXISTS + Tables.SCREENSHOT + "_inserted;");
            db.execSQL(CREATE_TRIGGER_IF_NOT_EXISTS + Tables.SCREENSHOT + "_inserted " +
                    "   AFTER INSERT ON " + Tables.SCREENSHOT +
                    " WHEN (SELECT count() FROM " + Tables.SCREENSHOT + ") > " + SCREENSHOT_LIMIT +
                    " BEGIN " +
                    "   DELETE FROM " + Tables.SCREENSHOT +
                    "     WHERE " + Screenshot._ID + " = " +
                    "(SELECT " + Screenshot._ID +
                    " FROM " + Tables.SCREENSHOT +
                    " ORDER BY " + Screenshot.TIMESTAMP +
                    " LIMIT 1);" +
                    " END");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private ScreenshotDatabaseHelper(Context context) {
        mOpenHelper = new OpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized ScreenshotDatabaseHelper getsInstacne(Context context) {
        if (sInstacne == null) {
            sInstacne = new ScreenshotDatabaseHelper(context);
        }
        return sInstacne;
    }

    public SQLiteDatabase getReadableDatabase() {
        return mOpenHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
        return mOpenHelper.getWritableDatabase();
    }
}
