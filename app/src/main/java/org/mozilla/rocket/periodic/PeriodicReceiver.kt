package org.mozilla.rocket.periodic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import java.util.concurrent.TimeUnit

class PeriodicReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        when (intent?.action) {
            FirstLaunchWorker.ACTION -> scheduleFirstLaunchWorker(context, WorkManager.getInstance())
        }
    }

    private fun scheduleFirstLaunchWorker(context: Context, workManager: WorkManager) {
        if (FirstLaunchWorker.isNotificationFired(context)) {
            return
        }

        val works = workManager.getWorkInfosByTag(FirstLaunchWorker.TAG).get()
        works.forEach {
            if (it?.state == WorkInfo.State.ENQUEUED) {
                /** Job already scheduled */
                return
            }
        }

        val config = AppConfigWrapper.getFirstLaunchWorkerTimer()
        val delayHoursToInstallTime = when (config.toInt()) {
            FirstLaunchWorker.TIMER_DISABLED -> {
                return
            }
            FirstLaunchWorker.TIMER_SUSPEND -> {
                return
            }
            else -> {
                if (config < 0) {
                    return
                }
                config
            }
        }
        val delayMinutes = calculateDelayMinutes(context, delayHoursToInstallTime)

        workManager.cancelAllWorkByTag(FirstLaunchWorker.TAG)
        val builder = OneTimeWorkRequest.Builder(FirstLaunchWorker::class.java)
        builder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        builder.addTag(FirstLaunchWorker.TAG)
        workManager.enqueue(builder.build())

        val message = AppConfigWrapper.getFirstLaunchNotificationMessage()
        TelemetryWrapper.receiveFirstrunConfig(delayMinutes, message)
    }

    private fun calculateDelayMinutes(context: Context, delayMinutesToInstallTime: Long): Long {
        /** Find next scheduled delay in minutes */
        val pm = context.packageManager
        var firstInstallTime: Long = Long.MAX_VALUE
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        if (packageInfo != null && packageInfo.packageName == context.packageName) {
            firstInstallTime = Math.min(firstInstallTime, packageInfo.firstInstallTime)
        }
        val delayMinutesRemain = delayMinutesToInstallTime - ((System.currentTimeMillis() - firstInstallTime) / (60000))
        val minutes: Long = when (delayMinutesRemain < 0 || delayMinutesRemain > delayMinutesToInstallTime) {
            true -> 0
            false -> delayMinutesRemain
        }

        return minutes
    }
}