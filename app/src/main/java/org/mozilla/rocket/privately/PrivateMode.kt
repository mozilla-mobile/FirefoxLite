/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.utils.FileUtils
import org.mozilla.focus.utils.ThreadUtils
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.tabs.TabViewProvider
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

        private const val PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT = false

        // Private Mode is currently behind a pref and is default off.
        // The option to enable it is on Nightly. The logic is in SettingsFragment.
        @JvmStatic
        fun isEnable(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PRIVATE_MODE_ENABLED, PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT)
        }

        // Clearing the cache will make the next webview startup using new Cookie and WebDatabase.
        @JvmStatic
        fun clearWebViewCache(context: Context) {
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
         * If this service is alive, it implies that there's a private session going on.
         * This is because this service is only started if there's a new private session.
         * This also implies there's a notification binding to this foreground service
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

        @JvmStatic
        fun isInPrivateProcess(context: Context): Boolean {

            val pid = android.os.Process.myPid()
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (processInfo in manager.runningAppProcesses) {
                if (processInfo.pid == pid) {
                    return processInfo.processName.contains(PRIVATE_PROCESS_NAME)
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

/**
 * A service to perform purify task.
 **
 */
class PrivateSessionBackgroundService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action ?: return Service.START_NOT_STICKY
        when (action) {
            ACTION_PURIFY -> callPurify()
            ACTION_END_PRIVATE_MODE -> callEndPrivateMode()
            else -> throw IllegalStateException("Unknown intent: $intent")
        }

        return Service.START_NOT_STICKY
    }

    private fun callEndPrivateMode() {
        TabViewProvider.purify(applicationContext)
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun callPurify() {
        TabViewProvider.purify(applicationContext)
    }

    companion object {

        private const val ACTION_PURIFY = "purify"
        private const val ACTION_END_PRIVATE_MODE = "end_private_mode"

        @JvmStatic
        fun purify(context: Context) {
            val intent = Intent(context, PrivateSessionBackgroundService::class.java)
            intent.action = ACTION_PURIFY
            context.startService(intent)
        }

        fun genEndingIntent(context: Context): Intent {
            val intent = Intent(context, PrivateSessionBackgroundService::class.java)
            intent.action = ACTION_END_PRIVATE_MODE
            return intent
        }
    }

}
