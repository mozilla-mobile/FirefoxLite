package org.mozilla.focus.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_BODY
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_IMAGE_URL
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_TITLE
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_MESSAGE_ID
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_COMMAND
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_DEEP_LINK
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_OPEN_URL
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.threadutils.ThreadUtils

class NotificationScheduleWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val messageId = inputData.getString(STR_MESSAGE_ID)
        val title = inputData.getString(STR_DATA_MSG_TITLE)
        val body = inputData.getString(STR_DATA_MSG_BODY)


        val openUrl = inputData.getString(STR_PUSH_OPEN_URL)
        val pushCommand = inputData.getString(STR_PUSH_COMMAND)
        val deepLink = inputData.getString(STR_PUSH_DEEP_LINK)
        val link = openUrl ?: pushCommand ?: deepLink ?: ""
        val imageUrl = inputData.getString(STR_DATA_MSG_IMAGE_URL)


        val pendingIntent = getClickPendingIntent(
                applicationContext,
                messageId,
                openUrl,
                pushCommand,
                deepLink
        )
        val builder = NotificationUtil.importantBuilder(context).setContentIntent(pendingIntent)
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

                                NotificationUtil.sendNotification(context, NotificationId.FIREBASE_AD_HOC, builder)
                                TelemetryWrapper.showNotification(link, messageId)
                            }
                        })
            }
        } else {
            NotificationUtil.sendNotification(context, NotificationId.FIREBASE_AD_HOC, builder)
            TelemetryWrapper.showNotification(link, messageId)
        }

        return Result.success()
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
}