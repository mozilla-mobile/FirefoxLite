/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import org.mozilla.focus.provider.SettingContract.GET_BOOLEAN
import org.mozilla.focus.provider.SettingContract.GET_FLOAT
import org.mozilla.focus.provider.SettingContract.KEY
import org.mozilla.focus.provider.SettingContract.PATH_GET_BOOLEAN
import org.mozilla.focus.provider.SettingContract.PATH_GET_FLOAT

class SettingProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        var key: String? = null
        var defValue = ""
        if (selectionArgs != null) {
            key = selectionArgs[0]
            defValue = selectionArgs[1]
        }
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val bundle = Bundle()
        when (uriMatcher.match(uri)) {
            GET_FLOAT -> bundle.putFloat(KEY, preferences.getFloat(key, java.lang.Float.parseFloat(defValue)))
            GET_BOOLEAN -> bundle.putBoolean(KEY, preferences.getBoolean(key, java.lang.Boolean.parseBoolean(defValue)))
            else -> throw IllegalArgumentException("Unknown uri：" + uri)
        }
        return BundleCursor(bundle)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not supported")
    }


    private class BundleCursor internal constructor(private var bundle: Bundle?) : MatrixCursor(arrayOf(), 0) {

        override fun getExtras(): Bundle? {
            return bundle
        }

        override fun respond(extras: Bundle): Bundle? {
            bundle = extras
            return bundle
        }
    }

    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(SettingContract.AUTHORITY, PATH_GET_FLOAT, GET_FLOAT)
            uriMatcher.addURI(SettingContract.AUTHORITY, PATH_GET_BOOLEAN, GET_BOOLEAN)
        }
    }
}
