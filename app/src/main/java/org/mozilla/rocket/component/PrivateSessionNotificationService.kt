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
import android.widget.Toast
import org.mozilla.focus.R
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.privately.PrivateSessionBackgroundService
import org.mozilla.rocket.tabs.TabViewProvider


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
        PrivateMode.hasPrivateSession.observeForever {
            it?.apply {
                if (it) {
                    showNotification()
                } else {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        PrivateMode.hasPrivateSession.value = false
        PrivateMode.hasPrivateModeActivity.value = false
        super.onTaskRemoved(rootIntent)
    }

    private fun showNotification() {

        val builder =
                NotificationUtil.baseBuilder(applicationContext, NotificationUtil.Channel.PRIVATE)
                        .setContentTitle(getString(R.string.private_browsing_erase_message))
                        .setContentIntent(buildPendingIntent(true))
                        .addAction(R.drawable.private_browsing_mask,
                                getString(R.string.private_browsing_open_action),
                                buildPendingIntent(false))

        startForeground(NotificationId.PRIVATE_MODE, builder.build())
    }

    private fun buildPendingIntent(sanitize: Boolean): PendingIntent {
        val intent = Intent(applicationContext, PrivateModeActivity::class.java)
        if (sanitize) {
            intent.action = PrivateMode.INTENT_EXTRA_SANITIZE
        }
        return PendingIntent.getActivity(applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
