/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.preference

import android.content.Context
import android.preference.PreferenceManager

interface PreferencesFactory {
    fun createPreferences(context: Context, name: String = ""): Preferences
}

class AndroidPreferencesFactory : PreferencesFactory {
    override fun createPreferences(context: Context, name: String): Preferences {
        return AndroidPreferences(if (name.isEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(context)
        } else {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        })
    }
}
