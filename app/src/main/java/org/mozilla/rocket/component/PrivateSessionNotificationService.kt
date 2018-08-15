/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.component

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import org.mozilla.focus.R
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.privately.PrivateModeActivity

/**
 * A service to toggle ConfigActivity on-off to clear Default browser config.
 *
 *
 * If the browser related packages list changed, it will clear default browser config. Hence all this
 * service doing is to enable then disable ConfigActivity.
 */

class PrivateSessionNotificationService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action ?: return Service.START_NOT_STICKY
        when (action) {
            ACTION_START -> showNotification()
            else -> throw IllegalStateException("Unknown intent: $intent")
        }

        return Service.START_NOT_STICKY
    }

    private fun showNotification() {

        val builder = NotificationUtil.baseBuilder(applicationContext, NotificationUtil.Channel.PRIVATE)
                .setContentTitle(getString(R.string.private_browsing_erase_message))
                .setContentIntent(buildPendingIntent(true))
                .addAction(R.drawable.private_browsing_mask, getString(R.string.private_browsing_open_action), buildPendingIntent(false))

        startForeground(NotificationId.PRIVATE_MODE, builder.build())
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val buildIntent = buildIntent(this, true)
        buildIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(buildIntent)
        super.onTaskRemoved(rootIntent)
    }

    private fun buildPendingIntent(sanitize: Boolean): PendingIntent {
        val intent = buildIntent(this, sanitize)
        return PendingIntent.getActivity(applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private val ACTION_START = "start"

        fun start(context: Context) {
            val intent = Intent(context, PrivateSessionNotificationService::class.java)
            intent.action = ACTION_START

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /* package */ internal fun stop(context: Context) {
            val intent = Intent(context, PrivateSessionNotificationService::class.java)
            context.stopService(intent)
        }

        @JvmStatic
        fun buildIntent(applicationContext: Context, sanitize: Boolean): Intent {
            val intent = Intent(applicationContext, PrivateModeActivity::class.java)
            if (sanitize) {
                intent.action = PrivateMode.INTENT_EXTRA_SANITIZE
            }
            return intent
        }
    }
}
