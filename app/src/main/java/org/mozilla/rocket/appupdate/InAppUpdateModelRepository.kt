/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import org.mozilla.focus.BuildConfig
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.Settings

class InAppUpdateModelRepository(
    private val settings: Settings
) : InAppUpdateManager.InAppUpdateModel {

    override fun getInAppUpdateRequestCode(): Int {
        return MainActivity.REQUEST_CODE_IN_APP_UPDATE
    }

    override fun isInAppUpdateLogEnabled(): Boolean {
        return BuildConfig.DEBUG || AppConstants.isNightlyBuild()
    }

    override fun getLastPromptInAppUpdateVersion(): Int {
        return settings.lastPromptInAppUpdateVersion
    }

    override fun setLastPromptInAppUpdateVersion(version: Int) {
        settings.lastPromptInAppUpdateVersion = version
    }
}
