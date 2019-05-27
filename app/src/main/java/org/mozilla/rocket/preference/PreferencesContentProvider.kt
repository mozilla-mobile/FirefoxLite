/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.preference

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import org.mozilla.focus.BuildConfig

class PreferencesContentProvider : ContentProvider() {
    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.preferencesprovider"

        private const val QUERY_PARAM_OP = "op"
        private const val QUERY_PARAM_KEY = "key"
        private const val QUERY_PARAM_VALUE = "value"

        const val OP_GET_STRING = 0
        const val OP_PUT_STRING = 1

        const val OP_GET_INT = 2
        const val OP_PUT_INT = 3

        const val OP_GET_LONG = 4
        const val OP_PUT_LONG = 5

        const val OP_GET_FLOAT = 6
        const val OP_PUT_FLOAT = 7

        const val OP_GET_BOOLEAN = 8
        const val OP_PUT_BOOLEAN = 9

        const val OP_OBSERVE = 10

        fun <T> makeUri(prefName: String, op: Int, key: String, value: T): Uri {
            val auth = "content://$AUTHORITY"
            return Uri.parse("$auth/$prefName?" +
                    "$QUERY_PARAM_OP=$op&" +
                    "$QUERY_PARAM_KEY=$key&" +
                    "$QUERY_PARAM_VALUE=$value")
        }

        inline fun <reified T> put(context: Context, prefName: String, key: String, value: T) {
            val op = when (value) {
                is String -> OP_PUT_STRING
                is Int -> OP_PUT_INT
                is Long -> OP_PUT_LONG
                is Float -> OP_PUT_FLOAT
                is Boolean -> OP_PUT_BOOLEAN
                else -> throw IllegalArgumentException("Unknown value type for key $key")
            }

            val uri = makeUri(prefName, op, key, value)
            context.contentResolver.update(uri, ContentValues(), null, null)
        }

        fun addObserver(context: Context, prefName: String, key: String, observer: ContentObserver) {
            val uri = makeUri(prefName, OP_OBSERVE, key, Any())
            context.contentResolver.registerContentObserver(uri, false, observer)
        }

        fun removeObserver(context: Context, observer: ContentObserver) {
            context.contentResolver.unregisterContentObserver(observer)
        }

        inline fun <reified T> get(
            context: Context,
            prefName: String,
            key: String,
            defaultValue: T
        ): T {
            val op = when (defaultValue) {
                is String -> OP_GET_STRING
                is Int -> OP_GET_INT
                is Long -> OP_GET_LONG
                is Float -> OP_GET_FLOAT
                is Boolean -> OP_GET_BOOLEAN
                else -> throw IllegalArgumentException("Unknown value type for key $key")
            }

            val uri = makeUri(prefName, op, key, defaultValue)
            context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
            )?.use {
                return it.extras.get(key) as T
            }
            return defaultValue
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val prefName = uri.path?.substring(1) ?: return null
        val op = uri.getQueryParameter(QUERY_PARAM_OP)?.toInt() ?: return null
        val key = uri.getQueryParameter(QUERY_PARAM_KEY) ?: return null
        val defaultValue = uri.getQueryParameter(QUERY_PARAM_VALUE) ?: return null

        val pref = getSharedPreferences(context, prefName)
        val bundle = Bundle()
        when (op) {
            OP_GET_STRING -> bundle.putString(key, pref.getString(key, defaultValue))
            OP_GET_INT -> bundle.putInt(key, pref.getInt(key, defaultValue.toInt()))
            OP_GET_LONG -> bundle.putLong(key, pref.getLong(key, defaultValue.toLong()))
            OP_GET_FLOAT -> bundle.putFloat(key, pref.getFloat(key, defaultValue.toFloat()))
            OP_GET_BOOLEAN -> bundle.putBoolean(key, pref.getBoolean(key, defaultValue.toBoolean()))
            else -> throw IllegalArgumentException("Unknown op: $op")
        }

        return BundleCursor(bundle)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val context = context ?: return 0
        val prefName = uri.path?.substring(1) ?: return 0
        val op = uri.getQueryParameter(QUERY_PARAM_OP)?.toInt() ?: return 0
        val key = uri.getQueryParameter(QUERY_PARAM_KEY) ?: return 0
        val value = uri.getQueryParameter(QUERY_PARAM_VALUE) ?: return 0

        val pref = getSharedPreferences(context, prefName)
        when (op) {
            OP_PUT_STRING -> pref.edit().putString(key, value).apply()
            OP_PUT_INT -> pref.edit().putInt(key, value.toInt()).apply()
            OP_PUT_LONG -> pref.edit().putLong(key, value.toLong()).apply()
            OP_PUT_FLOAT -> pref.edit().putFloat(key, value.toFloat()).apply()
            OP_PUT_BOOLEAN -> pref.edit().putBoolean(key, value.toBoolean()).apply()
            else -> throw IllegalArgumentException("Unknown op: $op")
        }

        val notifyUri = makeUri(prefName, OP_OBSERVE, key, Any())
        context.contentResolver.notifyChange(notifyUri, null)

        return 0
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("not implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("not implemented")
    }

    override fun getType(uri: Uri): String? {
        TODO("not implemented")
    }

    private fun getSharedPreferences(context: Context, name: String): SharedPreferences {
        return if (name.isEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(context)
        } else {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    private class BundleCursor internal constructor(private var bundle: Bundle?) : MatrixCursor(arrayOf(), 1) {

        override fun getExtras(): Bundle? {
            return bundle
        }

        override fun respond(extras: Bundle): Bundle? {
            bundle = extras
            return bundle
        }
    }
}