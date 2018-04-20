package org.mozilla.focus.utils;

import android.database.Cursor;

public class CursorUtils {

    public static void closeCursorSafely(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}
