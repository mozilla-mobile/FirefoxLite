/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import android.app.ActivityManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.threadutils.ThreadUtils
import java.io.File

// Describe when to clear the private mode session
class PrivateMode {

    // Provide common resources, and helper functions
    companion object {
        const val PREF_KEY_PRIVATE_MODE_ENABLED = "pref_key_private_mode_enabled"
        const val PREF_KEY_SANITIZE_REMINDER = "pref_key_sanitize_reminder"

        const val INTENT_EXTRA_SANITIZE = "intent_extra_sanitize"
        const val PRIVATE_PROCESS_NAME = "private_mode"
        const val WEBVIEW_FOLDER_NAME = "webview"

        // Private Mode is currently behind a pref and is default off.
        // The option to enable it is on Nightly. The logic is in SettingsFragment.
        @JvmStatic
        fun isEnable(context: Context): Boolean {
            // We look at Firebase Remote Config if in Release and Beta build type
            if (AppConstants.isReleaseBuild() || AppConstants.isBetaBuild()) {
                return AppConfigWrapper.isPrivateModeEnabled()
            }
            // In Debug and Firebase(debug) build type, enable Private Mode by default
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PRIVATE_MODE_ENABLED, true)
        }

        @JvmStatic
        fun sanitize(context: Context) {
            ThreadUtils.postToBackgroundThread {

                context.applicationContext.cacheDir?.let { dir ->
                    clean(dir, context)
                }

                context.applicationContext.getDir(PrivateMode.WEBVIEW_FOLDER_NAME, MODE_PRIVATE)?.let { dir ->
                    clean(dir, context)
                }
            }
        }

        /**
         * A helper function to report whether this service is alive.
         * When there's a private session, it implies a PrivateSessionNotificationService is running.
         *
         * @param context
         * @return true if this service is alive
         */
        @Suppress("deprecation")
        @JvmStatic
        fun hasPrivateSession(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Although this method is no longer available to third party applications.  For backwards compatibility,
            // it will still return the caller's own services.
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PrivateSessionNotificationService::class.java.name == service.service.className) {
                    return true
                }
            }

            return false
        }

        private fun clean(dir: File, context: Context) {

            val delete = FileUtils.deleteDirectory(dir)
            if (!delete) {
                // TODO:remember to clear the  SANITIZE_REMINDER when the app launch next time
                PreferenceManager.getDefaultSharedPreferences(context)?.edit()?.putString(PREF_KEY_SANITIZE_REMINDER, dir.absolutePath)?.apply()
            }
        }
    }
}