/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.preference

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper

class GlobalPreferencesFactory : PreferencesFactory {
    override fun createPreferences(context: Context, name: String): Preferences {
        return object : Preferences {
            private val observers = HashMap<() -> Unit, ContentObserver>()

            override fun putString(key: String, value: String) {
                PreferencesContentProvider.put(context, name, key, value)
            }

            override fun getString(key: String, defaultValue: String): String? {
                return PreferencesContentProvider.get(context, name, key, defaultValue)
            }

            override fun putInt(key: String, value: Int) {
                PreferencesContentProvider.put(context, name, key, value)
            }

            override fun getInt(key: String, defaultValue: Int): Int {
                return PreferencesContentProvider.get(context, name, key, defaultValue)
            }

            override fun putLong(key: String, value: Long) {
                PreferencesContentProvider.put(context, name, key, value)
            }

            override fun getLong(key: String, defaultValue: Long): Long {
                return PreferencesContentProvider.get(context, name, key, defaultValue)
            }

            override fun putFloat(key: String, value: Float) {
                PreferencesContentProvider.put(context, name, key, value)
            }

            override fun getFloat(key: String, defaultValue: Float): Float {
                return PreferencesContentProvider.get(context, name, key, defaultValue)
            }

            override fun putBoolean(key: String, value: Boolean) {
                PreferencesContentProvider.put(context, name, key, value)
            }

            override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
                return PreferencesContentProvider.get(context, name, key, defaultValue)
            }

            override fun addObserver(key: String, callback: () -> Unit) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        callback()
                    }
                }
                this.observers[callback] = observer
                PreferencesContentProvider.addObserver(context, name, key, observer)
            }

            override fun removeObserver(key: String, callback: () -> Unit) {
                this.observers.remove(callback)?.let {
                    PreferencesContentProvider.removeObserver(context, it)
                }
            }
        }
    }
}
