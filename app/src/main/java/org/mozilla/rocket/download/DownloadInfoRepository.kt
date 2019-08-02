package org.mozilla.rocket.download

import android.app.DownloadManager
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.threadutils.ThreadUtils

class DownloadInfoRepository {

    interface OnQueryListCompleteListener {
        fun onComplete(list: List<DownloadInfo>)
    }

    interface OnQueryItemCompleteListener {
        fun onComplete(download: DownloadInfo)
    }

    fun queryIndicatorStatus(listenerList: DownloadInfoRepository.OnQueryListCompleteListener) {
        DownloadInfoManager.getInstance().queryDownloadingAndUnreadIds { downloadInfoList ->
            listenerList.onComplete(downloadInfoList)
        }
    }

    fun queryByRowId(rowId: Long, listenerItem: OnQueryItemCompleteListener) {
        DownloadInfoManager.getInstance().queryByRowId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0) {
                val downloadInfo = downloadInfoList[0]
                listenerItem.onComplete(downloadInfo)
            }
        }
    }

    fun queryByDownloadId(rowId: Long, listenerItem: OnQueryItemCompleteListener) {
        DownloadInfoManager.getInstance().queryByDownloadId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0) {
                val downloadInfo = downloadInfoList[0]
                listenerItem.onComplete(downloadInfo)
            }
        }
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

    fun markAllItemsAreRead() {
        DownloadInfoManager.getInstance().markAllItemsAreRead(null)
    }

    fun loadData(offset: Int, pageSize: Int, listenerList: OnQueryListCompleteListener) {
        DownloadInfoManager.getInstance().query(offset, pageSize) { downloadInfoList ->
            listenerList.onComplete(downloadInfoList)
        }
    }

    fun remove(rowId: Long) {
        DownloadInfoManager.getInstance().delete(rowId, null)
    }

    fun deleteFromDownloadManager(downloadId: Long) {
        DownloadInfoManager.getInstance().downloadManager.remove(downloadId)
    }
}
