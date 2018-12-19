package org.mozilla.rocket.periodic

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.mozilla.focus.utils.DialogUtils

class FirstLaunchWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        val TAG: String = FirstLaunchWorker::class.java.simpleName

        private const val PREF_KEY_BOOLEAN_NOTIFICATION_FIRED: String = "pref-key-boolean-notification-fired"

        fun isNotificationFired(context: Context, default: Boolean = false): Boolean {
            return getSharedPreference(context).getBoolean(PREF_KEY_BOOLEAN_NOTIFICATION_FIRED, default)
        }

        fun setNotificationFired(context: Context, value: Boolean) {
            val edit = getSharedPreference(context).edit()
            edit.putBoolean(PREF_KEY_BOOLEAN_NOTIFICATION_FIRED, value)
            edit.apply()
        }

        private fun getSharedPreference(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

    override fun doWork(): Result {
        DialogUtils.showDefaultSettingNotification(applicationContext)
        setNotificationFired(applicationContext, true)
        return Result.success()
    }

}