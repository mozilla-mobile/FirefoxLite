package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.CursorUtils
import org.mozilla.threadutils.ThreadUtils

class DownloadStatusLiveData(ctx: Context) : LiveData<DownloadInfoBundle>() {
    lateinit var context: Context

    init {
        context = ctx.applicationContext
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
                if (id > 0) {
                    DownloadInfoManager.getInstance().queryByDownloadId(id, updateListener)
                }
            } else if (intent.action == DownloadInfoManager.ROW_UPDATED) {
                val id = intent.getLongExtra(DownloadInfoManager.ROW_ID, 0L)
                if (id > 0) {
                    DownloadInfoManager.getInstance().queryByRowId(id, updateListener)
                }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver, IntentFilter(DownloadInfoManager.ROW_UPDATED))
        context.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onInactive() {
        super.onInactive()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        context.unregisterReceiver(broadcastReceiver)
    }
}
