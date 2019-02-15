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

        val config = AppConfigWrapper.getFirstLaunchWorkerTimer(context).toInt()
        val delayHoursToInstallTime = when (config) {
            FirstLaunchWorker.TIMER_DISABLED -> {
                return
            }
            FirstLaunchWorker.TIMER_SUSPEND -> {
                return
            }
            else -> {
                config
            }
        }
        val delayHours: Long = calculateDelayHours(context, delayHoursToInstallTime)

        workManager.cancelAllWorkByTag(FirstLaunchWorker.TAG)
        val builder = OneTimeWorkRequest.Builder(FirstLaunchWorker::class.java)
        builder.setInitialDelay(delayHours, TimeUnit.HOURS)
        builder.addTag(FirstLaunchWorker.TAG)
        workManager.enqueue(builder.build())

        val message = AppConfigWrapper.getFirstLaunchNotificationiMessage(context)
        TelemetryWrapper.receiveFirstrunConfig(delayHours.toInt(), message)
    }

    private fun calculateDelayHours(context: Context, delayHoursToInstallTime: Int): Long {
        /** Find next scheduled hours */
        val pm = context.packageManager
        var firstInstallTime: Long = Long.MAX_VALUE
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        if (packageInfo != null && packageInfo.packageName == context.packageName) {
            firstInstallTime = Math.min(firstInstallTime, packageInfo.firstInstallTime)
        }
        val delayHoursRemain = delayHoursToInstallTime - ((System.currentTimeMillis() - firstInstallTime) / (3600000))
        val hours: Long = when (delayHoursRemain < 0 || delayHoursRemain > delayHoursToInstallTime) {
            true -> 1
            false -> delayHoursRemain
        }

        return hours
    }
}