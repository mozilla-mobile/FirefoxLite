package org.mozilla.rocket.component

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.utils.ThreadUtils
import org.mozilla.rocket.privately.Footprint
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.privately.PrivateModeListener


/**
 * A service to toggle ConfigActivity on-off to clear Default browser config.
 *
 *
 * If the browser related packages list changed, it will clear default browser config. Hence all this
 * service doing is to enable then disable ConfigActivity.
 */
private const val TAG = "PrivateSessionService"

class PrivateSessionNotificationService : Service(), PrivateModeListener {

    lateinit var privateMode: PrivateMode
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        val privateMode: PrivateMode
            get() = this@PrivateSessionNotificationService.privateMode
    }
    override fun onCreate() {
        super.onCreate()
        privateMode = PrivateMode(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onFirstObserver() {
        showNotification()
    }

    override fun onZeroObserver(footprint: Footprint) {

        stopForeground(true)


        ThreadUtils.postToBackgroundThread{
            for (file in footprint.filePath) {
                Log.d(TAG,"onZeroObserver:"+file.name)
                val delete = file.delete()
                if (!delete) {
                    // TODO:remember to clear the  SANITIZE_REMINDER when the app launch next time
                    PreferenceManager.getDefaultSharedPreferences(this)?.edit()?.putString(PrivateMode.PREF_KEY_SANITIZE_REMINDER, file.absolutePath)?.apply()
                }
            }
        }
    }

    private fun showNotification() {
        val builder = NotificationUtil.generateNotificationBuilder(applicationContext,buildIntent(MainActivity::class.java))

        builder.setContentTitle(getString(R.string.private_browsing_erase_message))

        val startPrivateModeActivity = buildIntent(PrivateModeActivity::class.java)
        builder.addAction(R.drawable.private_browsing_mask, getString(R.string.private_browsing_open_action), startPrivateModeActivity)

        startForeground(NotificationId.PRIVATE_MODE, builder.build())
    }


    private fun buildIntent(target: Class<*>): PendingIntent {
        val intent = Intent(applicationContext, target)
        intent.putExtra(PrivateMode.INTENT_EXTRA_SANITIZE, true)
        return PendingIntent.getActivity(applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

}
