package org.mozilla.rocket.msrp.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.debugging.DebugActivity
import org.mozilla.rocket.msrp.data.Mission
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyMissionReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val appContext: Context = context.applicationContext
    private val missionId: String? = params.inputData.getString(DATA_KEY_MISSION_ID)
    private val couponName: String? = params.inputData.getString(DATA_KEY_COUPON_NAME)
    private val expireTime: Long = params.inputData.getLong(DATA_KEY_EXPIRE_TIME, Long.MIN_VALUE)

    override fun doWork(): Result {
        if (isWorkerValid()) {
            showMissionReminderNotification(appContext, requireNotNull(couponName))
        } else {
            stopWorker(appContext)
        }

        return Result.success()
    }

    @SuppressLint("LongLogTag")
    private fun isWorkerValid(): Boolean {
        if (missionId == null) {
            Log.e(TAG, "missionId is null")
            return false
        }
        if (couponName == null) {
            Log.e(TAG, "couponName is null")
            return false
        }
        if (expireTime == Long.MIN_VALUE) {
            Log.e(TAG, "expireTime is not valid")
            return false
        }
        if (System.currentTimeMillis() > expireTime) {
            Log.e(TAG, "worker expired")
            return false
        }

        return true
    }

    private fun showMissionReminderNotification(appContext: Context, couponName: String) {
        val text = appContext.getString(R.string.msrp_notification_reminder, couponName)
        val largeIcon = DrawableUtils.getBitmap(ContextCompat.getDrawable(appContext, R.drawable.logo_man_push_notification))
        val pendingIntent = PendingIntent.getActivity(appContext, 0,
                Intent(appContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationUtil.baseBuilder(appContext, NotificationUtil.Channel.IMPORTANT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(couponName)
                .setContentText(text)
                .setLargeIcon(largeIcon)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(text)
                        .setBigContentTitle(couponName))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        NotificationUtil.sendNotification(appContext, NotificationId.MSRP_DAILY_MISSION_REMINDER, builder)
    }

    companion object {
        private const val TAG = "DailyMissionReminderWorker"
        private const val TAG_DAILY_MISSION_REMINDER_WORKER = "daily_mission_reminder_worker"
        private const val DATA_KEY_MISSION_ID = "mission_id"
        private const val DATA_KEY_COUPON_NAME = "coupon_name"
        private const val DATA_KEY_EXPIRE_TIME = "expire_time"
        private const val REMINDER_AIMED_HOUR = 19 // 7pm
        private val REPEAT_INTERVAL = TimeUnit.DAYS to 1L
        private val constraints: Constraints
            get() = Constraints.Builder()
                    .build()

        fun startMissionReminder(appContext: Context, mission: Mission) {
            val missionId = mission.uniqueId
            val couponName = mission.description
            val totalDays = mission.totalDays
            val uniqueWorkName = getUniqueWorkName(missionId)

            val isDebugEnabled = DebugActivity.isMissionReminderDebugEnabled

            val expiredDate = if (!isDebugEnabled) {
                Calendar.getInstance().apply {
                    add(Calendar.DATE, totalDays)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            } else {
                // Debug enabled
                Calendar.getInstance().timeInMillis + totalDays * DebugActivity.MISSION_REMINDER_DEBUG_REPEAT_INTERVAL.second * 60 * 1000L
            }
            val expireTime = if (mission.expiredDate < expiredDate) {
                mission.expiredDate
            } else {
                expiredDate
            }

            val initDelayMilli = if (!isDebugEnabled) {
                timeLeftHour(REMINDER_AIMED_HOUR, allowCurrentDay = false)
            } else {
                // Debug enabled
                DebugActivity.MISSION_REMINDER_DEBUG_REPEAT_INTERVAL.second * 60 * 1000L
            }

            val repeatIntervalTimeUnit = if (!isDebugEnabled) {
                REPEAT_INTERVAL.first
            } else {
                // Debug enabled
                DebugActivity.MISSION_REMINDER_DEBUG_REPEAT_INTERVAL.first
            }
            val repeatInterval = if (!isDebugEnabled) {
                REPEAT_INTERVAL.second
            } else {
                // Debug enabled
                DebugActivity.MISSION_REMINDER_DEBUG_REPEAT_INTERVAL.second
            }
            val reminderPeriodicWorkRequest = createMissionReminder(
                missionId,
                couponName,
                expireTime,
                initDelayMilli,
                repeatInterval,
                repeatIntervalTimeUnit
            )
            WorkManager.getInstance(appContext)
                    .enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.REPLACE, reminderPeriodicWorkRequest)
        }

        fun stopMissionReminder(appContext: Context, mission: Mission) {
            val uniqueWorkName = getUniqueWorkName(mission.uniqueId)
            WorkManager.getInstance(appContext)
                    .cancelUniqueWork(uniqueWorkName)
        }

        private fun getUniqueWorkName(missionId: String): String = "$TAG_DAILY_MISSION_REMINDER_WORKER$missionId"

        private fun createMissionReminder(
            missionId: String,
            couponName: String,
            expireTime: Long,
            initDelay: Long,
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit
        ): PeriodicWorkRequest {
            return PeriodicWorkRequest.Builder(
                        DailyMissionReminderWorker::class.java,
                        repeatInterval,
                        repeatIntervalTimeUnit,
                        repeatInterval,
                        repeatIntervalTimeUnit
                    )
                    .setInitialDelay(initDelay, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .setInputData(Data.Builder()
                            .putString(DATA_KEY_MISSION_ID, missionId)
                            .putString(DATA_KEY_COUPON_NAME, couponName)
                            .putLong(DATA_KEY_EXPIRE_TIME, expireTime)
                            .build())
                    .build()
        }

        private fun timeLeftHour(hourOfTheDay: Int, allowCurrentDay: Boolean): Long {
            val aimedTime = Calendar.getInstance().apply {
                if (!allowCurrentDay) {
                    set(Calendar.DATE, get(Calendar.DATE) + 1)
                }
                set(Calendar.HOUR_OF_DAY, hourOfTheDay)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val now = Calendar.getInstance()
            if (now.timeInMillis > aimedTime.timeInMillis) {
                aimedTime.timeInMillis = aimedTime.timeInMillis + TimeUnit.DAYS.toMillis(1)
            }

            return aimedTime.timeInMillis - now.timeInMillis
        }
    }
}

private fun Worker.stopWorker(appContext: Context) {
    WorkManager.getInstance(appContext)
            .cancelWorkById(id)
}