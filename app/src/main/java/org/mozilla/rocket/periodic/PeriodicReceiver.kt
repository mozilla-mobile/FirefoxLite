package org.mozilla.rocket.periodic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class PeriodicReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }
        scheduleFirstLaunchWorker(context, WorkManager.getInstance())
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

        /** This hours can configure by firebase */
        val delayHoursToInstallTime = 24

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

        workManager.cancelAllWorkByTag(FirstLaunchWorker.TAG)
        val builder = OneTimeWorkRequest.Builder(FirstLaunchWorker::class.java)
        builder.setInitialDelay(hours, TimeUnit.HOURS)
        builder.addTag(FirstLaunchWorker.TAG)
        workManager.enqueue(builder.build())
    }

}