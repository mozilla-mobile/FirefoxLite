package org.mozilla.focus.notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_BODY
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_IMAGE_URL
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_DATA_MSG_TITLE
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_MESSAGE_ID
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_COMMAND
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_DEEP_LINK
import org.mozilla.focus.notification.RocketMessagingService.Companion.STR_PUSH_OPEN_URL

class NotificationScheduleWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "FCM_WORKER"
    }

    override fun doWork(): Result {
        val messageId = inputData.getString(STR_MESSAGE_ID)
        val title = inputData.getString(STR_DATA_MSG_TITLE)
        Log.d(TAG, "Displaying title[$title] with messageId[$messageId]")
        val body = inputData.getString(STR_DATA_MSG_BODY)

        val openUrl = inputData.getString(STR_PUSH_OPEN_URL)
        val pushCommand = inputData.getString(STR_PUSH_COMMAND)
        val deepLink = inputData.getString(STR_PUSH_DEEP_LINK)
        val imageUrl = inputData.getString(STR_DATA_MSG_IMAGE_URL)

        RocketMessagingService.handlePushMessage(context.applicationContext, messageId, openUrl, pushCommand, deepLink, title, body, imageUrl)

        return Result.success()
    }
}