/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider

import android.net.Uri

import org.mozilla.focus.BuildConfig

object SettingContract {

    const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.settingprovider"
    val AUTHORITY_URI = Uri.parse("content://" + AUTHORITY)

    const val KEY = "key"
    const val PATH_GET_FLOAT = "getFloat"
    const val PATH_GET_BOOLEAN = "getBoolean"
    const val GET_FLOAT = 1
    const val GET_BOOLEAN = 2

}
