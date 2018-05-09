//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.mozilla.focus.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.mozilla.focus.utils.FirebaseHelper;

public class FirebaseHelperProvider extends ContentProvider {
    private static final String TAG = "CrashlyticsInitProvider";

    public FirebaseHelperProvider() {
    }

    public boolean onCreate() {
        final Context context = this.getContext();
        FirebaseHelper.init(context);

        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}
