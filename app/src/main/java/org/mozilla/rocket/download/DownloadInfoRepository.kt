package org.mozilla.rocket.download

import android.app.DownloadManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.data.DownloadInfo
import org.mozilla.rocket.download.data.DownloadInfoManager
import org.mozilla.threadutils.ThreadUtils

class DownloadInfoRepository {

    interface OnQueryListCompleteListener {
        fun onComplete(list: List<DownloadInfo>)
    }

    interface OnQueryItemCompleteListener {
        fun onComplete(download: DownloadInfo)
    }

    suspend fun queryIndicatorStatus(): List<DownloadInfo> =
        DownloadInfoManager.getInstance().queryDownloadingAndUnreadIds()

    fun queryByRowId(rowId: Long, listenerItem: OnQueryItemCompleteListener) {
        DownloadInfoManager.getInstance().queryByRowId(rowId, object : DownloadInfoManager.AsyncQueryListener {
            override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                if (downloadInfoList.isNotEmpty()) {
                    val downloadInfo = downloadInfoList[0]
                    listenerItem.onComplete(downloadInfo)
                }
            }
        })
    }

    fun queryByDownloadId(rowId: Long, listenerItem: OnQueryItemCompleteListener) {
        DownloadInfoManager.getInstance().queryByDownloadId(rowId, object : DownloadInfoManager.AsyncQueryListener {
            override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                if (downloadInfoList.isNotEmpty()) {
                    val downloadInfo = downloadInfoList[0]
                    listenerItem.onComplete(downloadInfo)
                }
            }
        })
    }

    fun queryDownloadingItems(runningIds: LongArray, listenerList: OnQueryListCompleteListener) {
        ThreadUtils.postToBackgroundThread {
            val query = DownloadManager.Query()
            query.setFilterById(*runningIds)
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING)
            DownloadInfoManager.getInstance().downloadManager.query(query).use {
                if (it != null) {
                    val list = ArrayList<DownloadInfo>()
                    while (it.moveToNext()) {
                        val id = it.getLong(it.getColumnIndex(DownloadManager.COLUMN_ID))
                        val totalSize = it.getDouble(it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val currentSize = it.getDouble(it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val info = DownloadInfo()
                        info.downloadId = id
                        info.sizeTotal = totalSize
                        info.sizeSoFar = currentSize
                        list.add(info)
                    }
                    listenerList.onComplete(list)
                }
            }
        }
    }

    suspend fun markAllItemsAreRead() =
        DownloadInfoManager.getInstance().markAllItemsAreRead()

    suspend fun loadData(offset: Int, pageSize: Int) =
        DownloadInfoManager.getInstance().query(offset, pageSize)

    fun remove(rowId: Long) {
        DownloadInfoManager.getInstance().delete(rowId, null)
    }

    fun deleteFromDownloadManager(downloadId: Long) {

        DownloadInfoManager.getInstance().downloadManager.remove(downloadId)
    }

    fun trackDownloadCancel(downloadId: Long) {
        val downloadPojo =
            DownloadInfoManager.getInstance().queryDownloadManager(downloadId) ?: return
        val progress = if (downloadPojo.length == 0L) {
            0.0
        } else {
            downloadPojo.sizeSoFar.times(100).toDouble() / downloadPojo.length
        }
        // track the event when the file download cancel from Download Panel.
        TelemetryWrapper.endDownloadFile(
            downloadId,
            downloadPojo.length,
            progress,
            DownloadInfo.STATUS_DELETED,
            DownloadInfo.REASON_DEFAULT
        )
    }
}
