/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.mozilla.focus.telemetry.TelemetryWrapper.getNotification
import org.mozilla.focus.telemetry.TelemetryWrapper.isTelemetryEnabled
import org.mozilla.focus.telemetry.TelemetryWrapper.showNotification
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.threadutils.ThreadUtils

// Prov
class RocketMessagingService : FirebaseMessagingServiceWrapper() {
    //
    override fun onRemoteMessage(intent: Intent, title: String?, body: String?) {
        val messageId = parseMessageId(intent)
        val link = parseLink(intent)
        getNotification(link, messageId)
        if (!isTelemetryEnabled(this)) {
            return
        }
        val pendingIntent = getClickPendingIntent(
                applicationContext,
                messageId,
                parseOpenUrl(intent),
                parseCommand(intent),
                parseDeepLink(intent)
        )
        val builder = NotificationUtil.importantBuilder(this).setContentIntent(pendingIntent)
        title?.let { builder.setContentTitle(it) }
        body?.let { builder.setContentText(it) }
        addDeleteTelemetry(applicationContext, builder, messageId, link)

        val imageUrl = parseImageUrl(intent)
        if (!imageUrl.isNullOrEmpty()) {
            ThreadUtils.postToMainThread {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(imageUrl)
                    .into(object : SimpleTarget<Bitmap?>() {
                        override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap?>?) {
                            builder.setLargeIcon(resource)
                            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(resource))

                            NotificationUtil.sendNotification(this@RocketMessagingService, NotificationId.FIREBASE_AD_HOC, builder)
                            showNotification(link, messageId)
                        }
                    })
            }
        } else {
            NotificationUtil.sendNotification(this, NotificationId.FIREBASE_AD_HOC, builder)
            showNotification(link, messageId)
        }
    }

    private fun parseMessageId(intent: Intent): String? {
        return intent.getStringExtra(MESSAGE_ID)
    }

    private fun parseOpenUrl(intent: Intent): String? {
        return intent.getStringExtra(PUSH_OPEN_URL)
    }

    private fun parseCommand(intent: Intent): String? {
        return intent.getStringExtra(PUSH_COMMAND)
    }

    private fun parseDeepLink(intent: Intent): String? {
        return intent.getStringExtra(PUSH_DEEP_LINK)
    }

    private fun parseLink(intent: Intent): String? {
        var link = intent.getStringExtra(PUSH_OPEN_URL)
        if (link == null) {
            link = intent.getStringExtra(PUSH_COMMAND)
        }
        if (link == null) {
            link = intent.getStringExtra(PUSH_DEEP_LINK)
        }
        return link
    }

    private fun parseImageUrl(intent: Intent): String? {
        return intent.getStringExtra(PUSH_IMAGE_URL)
    }

    private fun getClickPendingIntent(appContext: Context, messageId: String?, openUrl: String?, command: String?, deepLink: String?): PendingIntent { // RocketLauncherActivity will handle this intent
        val clickIntent = IntentUtils.genFirebaseNotificationClickForBroadcastReceiver(
                appContext,
                messageId,
                openUrl,
                command,
                deepLink
        )
        return PendingIntent.getBroadcast(this, REQUEST_CODE_CLICK_NOTIFICATION, clickIntent, PendingIntent.FLAG_ONE_SHOT)
    }

    private fun addDeleteTelemetry(appContext: Context, builder: NotificationCompat.Builder, messageId: String?, link: String?) {
        val intent = IntentUtils.genDeleteFirebaseNotificationActionForBroadcastReceiver(appContext, messageId, link)
        val pendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_CODE_DELETE_NOTIFICATION, intent, PendingIntent.FLAG_ONE_SHOT)
        builder.setDeleteIntent(pendingIntent)
    }

    companion object {
        private const val REQUEST_CODE_CLICK_NOTIFICATION = 1
        private const val REQUEST_CODE_DELETE_NOTIFICATION = 2
    }
}
