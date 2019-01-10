package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager
import org.mozilla.focus.Inject
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager

class DownloadBroadcastReceiver : BroadcastReceiver() {
    var downloadInfoBundle = MutableLiveData<DownloadInfoBundle>()

    init {
        downloadInfoBundle.value = DownloadInfoBundle(ArrayList(), -1, -1)
    }

    private var updateListener = { downloadInfoList: List<DownloadInfo> ->
        for (downloadInfo in downloadInfoList) {
            updateItem(downloadInfo)
        }
    }

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

    private fun updateItem(downloadInfo: DownloadInfo) {
        var index = -1
        val list = downloadInfoBundle.value?.list
        for (i in 0 until list?.size!!) {
            if (list[i].rowId == downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            list.add(0, downloadInfo)
        } else {
            list.removeAt(index)
            list.add(index, downloadInfo)
        }
        downloadInfoBundle.value?.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
    }

    companion object {
        var instance: DownloadBroadcastReceiver? = null

        @JvmStatic
        fun onResume(activity: FragmentActivity) {
            instance = DownloadBroadcastReceiver()
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(instance!!)
            activity.unregisterReceiver(instance)
            Inject.provideDownloadInfoRepository(activity.application).removeDataSource(instance!!.downloadInfoBundle)
        }

        @JvmStatic
        fun onPause(activity: FragmentActivity) {

            if (instance == null) return
            LocalBroadcastManager.getInstance(activity)
                .registerReceiver(instance!!, IntentFilter(DownloadInfoManager.ROW_UPDATED))

            activity.registerReceiver(instance, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            Inject.provideDownloadInfoRepository(activity.application)
                .addDataSource(instance!!.downloadInfoBundle)
        }
    }
}