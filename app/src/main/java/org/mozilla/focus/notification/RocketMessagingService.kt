/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.telemetry.TelemetryWrapper.getNotification
import org.mozilla.focus.telemetry.TelemetryWrapper.isTelemetryEnabled
import org.mozilla.focus.telemetry.TelemetryWrapper.showNotification
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.threadutils.ThreadUtils
import java.util.concurrent.TimeUnit

// Prov
class RocketMessagingService : FirebaseMessagingServiceWrapper() {
    //
    override fun onNotificationMessage(data: Map<String, String>, title: String?, body: String?, imageUrl: String?) {
        val messageId = parseMessageId(data)
        val link = parseLink(data)
        getNotification(link, messageId)
        if (!isTelemetryEnabled(this)) {
            return
        }
        val pendingIntent = getClickPendingIntent(
                applicationContext,
                messageId,
                parseOpenUrl(data),
                parseCommand(data),
                parseDeepLink(data)
        )
        val builder = NotificationUtil.importantBuilder(this).setContentIntent(pendingIntent)
        title?.let { builder.setContentTitle(it) }
        body?.let { builder.setContentText(it) }
        addDeleteTelemetry(applicationContext, builder, messageId, link)

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

    override fun onDataMessage(data: MutableMap<String, String>) {

        val messageId = parseMessageId(data)

        val title = data[STR_DATA_MSG_TITLE]
        val body = data[STR_DATA_MSG_BODY]
        val openUrl = data[STR_PUSH_OPEN_URL]
        val pushCommand = data[STR_PUSH_COMMAND]
        val deepLink = data[STR_PUSH_DEEP_LINK]
        val displayType = data[STR_DATA_MSG_DISPLAY_TYPE]   // we only have one time now
        val displayTimestamp = data[LONG_DATA_MSG_DISPLAY_TIMESTAMP]?.toLong()

        if (messageId == null || title == null || body == null || displayTimestamp == null || displayType == null) {
            return
        }

        val link = parseLink(data)
        getNotification(link, messageId)

        val imageUri = data[STR_DATA_MSG_IMAGE_URL]
        if (imageUri != null && !URLUtil.isValidUrl(imageUri)) {
            return
        }
        val inputDataBuilder = Data.Builder()
                .putString(STR_MESSAGE_ID, messageId)
                .putString(STR_DATA_MSG_TITLE, title)
                .putString(STR_DATA_MSG_BODY, body)
                .putString(STR_PUSH_OPEN_URL, openUrl)
                .putString(STR_PUSH_COMMAND, pushCommand)
                .putString(STR_PUSH_DEEP_LINK, deepLink)


        if ((imageUri != null)) {
            inputDataBuilder.putString(STR_DATA_MSG_IMAGE_URL, imageUri)
        }

        val request =
                OneTimeWorkRequest.Builder(NotificationScheduleWorker::class.java)
                        .setInputData(inputDataBuilder.build())
                        .setInitialDelay(displayTimestamp-System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .addTag(messageId)
                        .build()

        WorkManager.getInstance(this).enqueue(request)

        for (entry in data) {
            if (BuildConfig.DEBUG) {
                Log.d("FCM", "onDataMessage---[${entry.key}]----${entry.value}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
//        place holder for the server side
//        HttpURLConnectionClient()
//                .withInterceptors(LoggingInterceptor())
//                .fetch(Request(
//                        url = ""
//                )).use {
//                    Log.d("FirebaseAaaa", "[$token]----${it.body.toString()}")
//                }
    }

    private fun parseMessageId(data: Map<String, String>): String? {
        return data[STR_MESSAGE_ID]
    }

    private fun parseOpenUrl(data: Map<String, String>): String? {
        return data[STR_PUSH_OPEN_URL]
    }

    private fun parseCommand(data: Map<String, String>): String? {
        return data[STR_PUSH_COMMAND]
    }

    private fun parseDeepLink(data: Map<String, String>): String? {
        return data[STR_PUSH_DEEP_LINK]
    }

    private fun parseLink(data: Map<String, String>): String? {
        var link = data[STR_PUSH_OPEN_URL]
        if (link == null) {
            link = data[STR_PUSH_COMMAND]
        }
        if (link == null) {
            link = data[STR_PUSH_DEEP_LINK]
        }
        return link
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
        const val REQUEST_CODE_CLICK_NOTIFICATION = 1
        const val REQUEST_CODE_DELETE_NOTIFICATION = 2

        // shared between data and notification message
        const val STR_MESSAGE_ID = "message_id"
        const val STR_PUSH_OPEN_URL = "push_open_url"
        const val STR_PUSH_COMMAND = "push_command"
        const val STR_PUSH_DEEP_LINK = "push_deep_link"

        // data message only
        const val STR_DATA_MSG_TITLE = "title"
        const val STR_DATA_MSG_BODY = "body"
        const val STR_DATA_MSG_DISPLAY_TYPE = "display_type"
        const val LONG_DATA_MSG_DISPLAY_TIMESTAMP = "display_timestamp"
        const val STR_DATA_MSG_IMAGE_URL = "image_uri"
    }
}
