package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.provider.DownloadContract

class DownloadIndicatorLiveData(private val context: Context) : LiveData<Int>(), DownloadInfoManager.AsyncQueryListener {

    object Constants {
        const val STATUS_DEFAULT = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_UNREAD = 2
    }

    private val contentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            super.onChange(selfChange, uri)
            forceQuery()
        }
    }

    override fun onActive() {
        super.onActive()
        context.contentResolver.registerContentObserver(DownloadContract.Download.CONTENT_URI, true, contentObserver)
        forceQuery()
    }

    override fun onInactive() {
        super.onInactive()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
        val list = ArrayList<DownloadInfo>()
        list.addAll(downloadInfoList)

        var hasDownloading = false
        var hasUnread = false
        for (item in list) {
            if (!hasDownloading && (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING)) {
                hasDownloading = true
            }
            if (!hasUnread && item.status == DownloadManager.STATUS_SUCCESSFUL &&!item.isRead) {
                hasUnread = true
            }
        }
        value = when {
            hasDownloading -> Constants.STATUS_DOWNLOADING
            hasUnread -> Constants.STATUS_UNREAD
            else -> Constants.STATUS_DEFAULT
        }
    }

    fun forceQuery() {
        DownloadInfoManager.getInstance().queryDownloadingAndUnreadIds(this)
    }
}
