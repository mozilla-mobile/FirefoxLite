/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.settings

import android.content.Context
import org.mozilla.rocket.preference.PreferencesFactory

class PrivateBrowsingSettings(context: Context, prefFactory: PreferencesFactory) {
    companion object {
        private const val PREF_NAME = "private_browsing"
    }

    val preferences = prefFactory.createPreferences(context, PREF_NAME)
}
