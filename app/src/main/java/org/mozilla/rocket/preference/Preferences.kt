/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.preference

import android.content.SharedPreferences

interface Preferences {
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String): String?

    fun putInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int): Int

    fun putLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long): Long

    fun putFloat(key: String, value: Float)
    fun getFloat(key: String, defaultValue: Float): Float

    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun addObserver(key: String, callback: () -> Unit) {
        TODO("not implemented")
    }

    fun removeObserver(key: String, callback: () -> Unit) {
        TODO("not implemented")
    }
}

class AndroidPreferences(private val delegate: SharedPreferences) : Preferences {
    override fun putString(key: String, value: String) {
        delegate.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String? {
        return delegate.getString(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        delegate.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return delegate.getInt(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        delegate.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return delegate.getLong(key, defaultValue)
    }

    override fun putFloat(key: String, value: Float) {
        delegate.edit().putFloat(key, value).apply()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return delegate.getFloat(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        delegate.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return delegate.getBoolean(key, defaultValue)
    }
}
