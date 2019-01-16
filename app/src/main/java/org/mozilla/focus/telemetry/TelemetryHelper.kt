/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.telemetry

import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager

import kotlin.annotation.Retention

/*
* State the date to remove this function.
*
* */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Due(
    val date: String,
    val reason: String = ""
)

/**
 *  Some helper method for Telemetry
 */
class TelemetryHelper {

    companion object {

        @JvmStatic
        private var isFirstLaunch = false // the app is in a state when it's first launched after installation

        // the app is in a state when it's Firebase Remote Config is fetched. It's only useful when the app is launched
        // for the first time. Because the successive launch may not fetch the remote config due to it's time threshold.
        var remoteConfigFetched = false

        // pref key used to make sure
        private const val FIRST_LAUNCH_VERSION = "first_launch_version"

        const val RC_READY_REMOTE_CONFIG_FETCHED = "rc_firebase_fetched"
        const val RC_READY_FIRST_RUN_SHOWN = "rc_first_show"
        const val RC_READY_FIRST_RUN_END = "rc_first_end"
        const val RC_READY_FIRST_SHOW_HOME = "rc_home1_show"
        const val RC_READY_FIRST_SHOW_HOME_MEU = "rc_home2_show"
        const val RC_READY_FIRST_SHOW_BROWSER = "rc_browser1_show"
        const val RC_READY_FIRST_SHOW_BROWSER_MENU = "rc_browser2_show"

        fun onFirstLaunch(context: Context?, key: String, function: () -> Unit) {
            if (!isFirstLaunch) {
                return
            }
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getBoolean(key, true)) {
                function()
                preferences.edit().putBoolean(key, false).apply()
            }
        }

        fun firstLaunchVersion(context: Context?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getString(FIRST_LAUNCH_VERSION, null) == null) {
                try {
                    context?.packageManager?.getPackageInfo(context.packageName, 0)?.versionName?.apply {
                        preferences.edit().putString(FIRST_LAUNCH_VERSION, this).apply()
                    }
                    isFirstLaunch = true
                } catch (e: PackageManager.NameNotFoundException) {
                    // Nothing to do if we can't find the package name.
                }
            }
        }
    }
}
