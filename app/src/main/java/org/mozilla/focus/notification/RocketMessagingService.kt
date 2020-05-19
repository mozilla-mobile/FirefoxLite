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
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.interceptor.withInterceptors
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.isTelemetryEnabled
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.msrp.data.LoggingInterceptor
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.threadutils.ThreadUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handle Notification & Data Message from FCM
 * */
class RocketMessagingService : FirebaseMessagingServiceWrapper() {

    override fun onNotificationMessage(data: Map<String, String>, title: String?, body: String?, imageUrl: String?) {
        val messageId = parseMessageId(data)
        val openUrl = parseOpenUrl(data)
        val pushCommand = parseCommand(data)
        val deepLink = parseDeepLink(data)
        Log.d(TAG, "onNotificationMessage messageId:$$messageId ")

        val link = parseLink(openUrl, pushCommand, deepLink)
        TelemetryWrapper.getNotification(link, messageId)

        handlePushMessage(applicationContext, messageId, openUrl, pushCommand, deepLink, title, body, imageUrl)
    }

    override fun onDataMessage(data: MutableMap<String, String>) {

        val messageId = parseMessageId(data)
        val title = parseTitle(data)
        val body = parseBody(data)
        val openUrl = parseOpenUrl(data)
        val pushCommand = parseCommand(data)
        val deepLink = parseDeepLink(data)
        val displayType = parseDisplayType(data)
        val displayTimestamp = parseDisplayTimestamp(data)

        val link = parseLink(openUrl, pushCommand, deepLink)
        TelemetryWrapper.getNotification(link, messageId)

        if (messageId == null || title == null || body == null || displayTimestamp == null || displayType == null) {
            Log.d(TAG, "onDataMessage failed. Required field missing")
            return
        }

        val serverPushEnabled = FirebaseHelper.getFirebase().getRcBoolean(BOOL_IS_SERVER_PUSH_ENABLED)
        val serverPushDebugging = Settings.getInstance(applicationContext).isServerPushDebugging
        Log.d(TAG, "onDataMessage enabled:$serverPushEnabled ")
        if (!serverPushEnabled && !serverPushDebugging) {
            // return early if we pref off this feature in Firebase Remote Config
            return
        }

        val imageUrl = parseImageUrl(data)

        scheduleNotification(applicationContext, messageId, imageUrl, title, body, openUrl, pushCommand, deepLink, displayTimestamp)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken")
        handleNewToken(applicationContext, token)
    }

    private fun parseMessageId(data: Map<String, String>): String? {
        return data[STR_MESSAGE_ID]
    }

    private fun parseTitle(data: Map<String, String>): String? {
        return data[STR_DATA_MSG_TITLE]
    }

    private fun parseBody(data: Map<String, String>): String? {
        return data[STR_DATA_MSG_BODY]
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

    private fun parseDisplayType(data: Map<String, String>): String? {
        return data[STR_DATA_MSG_DISPLAY_TYPE]
    }

    private fun parseDisplayTimestamp(data: Map<String, String>): Long? {
        return data[LONG_DATA_MSG_DISPLAY_TIMESTAMP]?.toLong()
    }

    private fun parseImageUrl(data: Map<String, String>): String? {
        return data[STR_DATA_MSG_IMAGE_URL]
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
        const val STR_DATA_MSG_IMAGE_URL = "image_url"

        private const val TAG = "FCM_SERVICE"
        private const val STR_USER_TOKEN_API = "str_user_token_api"
        private const val BOOL_IS_SERVER_PUSH_ENABLED = "bool_is_server_push_enabled"

        fun scheduleNotification(applicationContext: Context, messageId: String, imageUrl: String?, title: String?, body: String?, openUrl: String?, pushCommand: String?, deepLink: String?, displayTimestamp: Long) {

            val inputDataBuilder = Data.Builder()
                    .putString(STR_MESSAGE_ID, messageId)
                    .putString(STR_DATA_MSG_TITLE, title)
                    .putString(STR_DATA_MSG_BODY, body)
                    .putString(STR_PUSH_OPEN_URL, openUrl)
                    .putString(STR_PUSH_COMMAND, pushCommand)
                    .putString(STR_PUSH_DEEP_LINK, deepLink)

            // only allow valid urls
            if (imageUrl != null && URLUtil.isValidUrl(imageUrl)) {
                Log.d(TAG, "Setting imageUrl")
                inputDataBuilder.putString(STR_DATA_MSG_IMAGE_URL, imageUrl)
            }

            val delay = displayTimestamp - System.currentTimeMillis()
            if (delay < 0) {
                Log.d(TAG, "Request delay [$delay] ms was due")
                return // if the message comes late, ignore it.
            }

            Log.d(TAG, "Schedule display notification [$title] from server [$delay] ms later.")
            val request =
                    OneTimeWorkRequest.Builder(NotificationScheduleWorker::class.java)
                            .setInputData(inputDataBuilder.build())
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .addTag(messageId)
                            .build()

            WorkManager.getInstance(applicationContext).enqueue(request)
        }

        fun handlePushMessage(applicationContext: Context, messageId: String?, openUrl: String?, pushCommand: String?, deepLink: String?, title: String?, body: String?, imageUrl: String?) {

            val pendingIntent = getClickPendingIntent(
                    applicationContext,
                    messageId,
                    openUrl,
                    pushCommand,
                    deepLink
            )
            val builder = NotificationUtil.importantBuilder(applicationContext).setContentIntent(pendingIntent)
            title?.let { builder.setContentTitle(it) }
            body?.let { builder.setContentText(it) }

            val link = parseLink(openUrl, pushCommand, deepLink)

            if (!isTelemetryEnabled(applicationContext)) {
                return
            }

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

                                    NotificationUtil.sendNotification(applicationContext, NotificationId.FIREBASE_AD_HOC, builder)
                                    TelemetryWrapper.showNotification(link, messageId)
                                }
                            })
                }
            } else {
                NotificationUtil.sendNotification(applicationContext, NotificationId.FIREBASE_AD_HOC, builder)
                TelemetryWrapper.showNotification(link, messageId)
            }
        }

        private fun parseLink(openUrl: String?, pushCommand: String?, deepLink: String?): String? {
            var link = openUrl
            if (link == null) {
                link = pushCommand
            }
            if (link == null) {
                link = deepLink
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
            return PendingIntent.getBroadcast(appContext, RocketMessagingService.REQUEST_CODE_CLICK_NOTIFICATION, clickIntent, PendingIntent.FLAG_ONE_SHOT)
        }

        private fun addDeleteTelemetry(appContext: Context, builder: NotificationCompat.Builder, messageId: String?, link: String?) {
            val intent = IntentUtils.genDeleteFirebaseNotificationActionForBroadcastReceiver(appContext, messageId, link)
            val pendingIntent = PendingIntent.getBroadcast(appContext, RocketMessagingService.REQUEST_CODE_DELETE_NOTIFICATION, intent, PendingIntent.FLAG_ONE_SHOT)
            builder.setDeleteIntent(pendingIntent)
        }

        fun checkFcmTokenUploaded(applicationContext: Context) {
            val hashedFcmToken = Settings.getInstance(applicationContext).hashedFcmToken
            val currentFcmToken = FirebaseHelper.getFirebase().getFcmToken()

            if (currentFcmToken == null) {
                Log.w(TAG, "currentFcmToken is null. Wait for it and retry")
                return
            }
            if (hashedFcmToken != currentFcmToken.hashCode()) {
                Log.d(TAG, "handleNewToken....")
                handleNewToken(applicationContext, currentFcmToken)
            } else {
                Log.w(TAG, "token not changed")
            }
        }

        private fun handleNewToken(applicationContext: Context, token: String) {
            FirebaseHelper.getFirebase().getUserToken {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        it?.apply {
                            sendRegistrationToServer(applicationContext, this, token)
                        }
                    }
                }
            }
        }

        @WorkerThread
        private fun sendRegistrationToServer(applicationContext: Context, fbUid: String, fcmToken: String) {
            Log.d(TAG, "sendRegistrationToServer with token")
            val telemetryClientId = TelemetryHolder.get().clientId
            if (telemetryClientId == null) {
                Log.w(TAG, "telemetryClientId is null")
                return
            }

            // something like //"http://10.0.2.2:8080/api/v1/user/token"
            val userTokenApiUrl = FirebaseHelper.getFirebase().getRcString(STR_USER_TOKEN_API)
            if (userTokenApiUrl.isEmpty()) {
                Log.w(TAG, "userTokenApiUrl is empty. Wait for RemoteConfig and retry")
                return
            }
            Log.d(TAG, "userTokenApiUrl = $userTokenApiUrl")
            val request = Request(
                    url = userTokenApiUrl,
                    headers = MutableHeaders(
                            "Authorization" to "Bearer $fbUid"
                    ),
                    body = Request.Body.fromParamsForFormUrlEncoded(
                            "telemetry_client_id" to telemetryClientId,
                            "fcm_token" to fcmToken
                    )
            )
            try {

                HttpURLConnectionClient()
                        .withInterceptors(LoggingInterceptor())
                        .fetch(request).use {
                            if (it.status == 200) {
                                Settings.getInstance(applicationContext).setHashedFcmToken(fcmToken)
                                Log.d(TAG, "FCM Token uploaded: ${fcmToken.hashCode()}")
                            } else {
                                Log.d(TAG, "FCM Token uploaded status[${it.status}]: [${it.body}]")
                            }
                        }
            } catch (e: IOException) {
                Log.e(TAG, "FCM Token upload ERROR: [${fcmToken.hashCode()}] with [${e.localizedMessage}]")
            }
        }
    }
}
