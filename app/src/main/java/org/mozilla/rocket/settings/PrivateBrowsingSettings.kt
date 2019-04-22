/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.settings

import android.content.Context
import org.mozilla.focus.R
import org.mozilla.rocket.preference.PreferencesFactory

class PrivateBrowsingSettings(context: Context, prefFactory: PreferencesFactory) {
    companion object {
        private const val PREF_NAME = "private_browsing"

        private const val DEFAULT_TURBO_MODE_ENABLED = true
    }

    private val preferences = prefFactory.createPreferences(context, PREF_NAME)
    private val resources = context.resources

    fun shouldUseTurboMode() = preferences.getBoolean(
            resources.getString(R.string.pref_key_pb_boolean_turbo_enabled),
            DEFAULT_TURBO_MODE_ENABLED
    )

    fun setTurboMode(enabled: Boolean) {
        preferences.putBoolean(resources.getString(R.string.pref_key_pb_boolean_turbo_enabled), enabled)
    }
}
