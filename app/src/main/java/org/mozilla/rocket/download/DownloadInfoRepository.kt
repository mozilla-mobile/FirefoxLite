package org.mozilla.rocket.download

import android.app.Application
import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.content.Context
import android.database.Cursor
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.CursorUtils
import org.mozilla.threadutils.ThreadUtils

// fixit before ship
lateinit var downloadManager: DownloadManager

class DownloadInfoRepository {

    var downloadIndicator: DownloadIndicatorLiveData? = null

    fun getDownloadIndicator(): LiveData<Int>? {
        return downloadIndicator
    }

    fun updateIndicator() {
        downloadIndicator?.forceQuery()
    }

    fun markAllItemsAreRead() {
        DownloadInfoManager.getInstance().markAllItemsAreRead(null)
    }

    fun addDataSource(data: LiveData<DownloadInfoBundle>) {
        downloadInfoBundle.addSource(data, downloadInfoBundle::setValue)
    }

    fun removeDataSource(data: LiveData<DownloadInfoBundle>) {
        downloadInfoBundle.removeSource(data)
    }

    companion object {

        const val QUERY_PROGRESS_DELAY: Long = 500
        private val PAGE_SIZE = 20

        @Volatile
        private var INSTANCE: DownloadInfoRepository? = null

        @JvmStatic
        fun getInstance(application: Application): DownloadInfoRepository? {
            downloadManager =
                    application.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadInfoRepository().also {
                    INSTANCE = it
                    INSTANCE?.downloadIndicator = DownloadIndicatorLiveData(application.applicationContext)
                }
            }
        }
    }

    fun loadMore(itemCount: Int, listener: DownloadInfoManager.AsyncQueryListener) {

        DownloadInfoManager.getInstance().query(itemCount, PAGE_SIZE, listener)
    }

//    private fun updateRunningItems() {
//        for (i in runningDownloadIds.indices) {
//            DownloadInfoManager.getInstance().queryByDownloadId(runningDownloadIds[i], updateListener)
//        }
//    }

    fun removeItem(rowId: Long) {
        DownloadInfoManager.getInstance().delete(rowId, null)
    }

    fun delete(rowId: Long, listener: DownloadInfoViewModel.OnOperationComplete?) {
        DownloadInfoManager.getInstance().queryByRowId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0 && rowId == (downloadInfoList[0] as DownloadInfo).rowId) {

                val deletedDownload = downloadInfoList[0] as DownloadInfo
                listener?.onComplete(deletedDownload)
            }
        }
    }

    fun cancel(rowId: Long, listener: DownloadInfoViewModel.OnOperationComplete?) {

        DownloadInfoManager.getInstance().queryByRowId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0) {
                val downloadInfo = downloadInfoList[0]

                if (downloadInfo.existInDownloadManager()) {
                    if (rowId == downloadInfo.rowId && DownloadManager.STATUS_SUCCESSFUL != downloadInfo.status) {

                        listener?.onComplete(downloadInfo)

                        downloadManager.remove(downloadInfo.downloadId)

                        removeItem(rowId)
                    }
                }
            }
        }
    }


    fun queryDownloadProgress(
        runningIds: LongArray,
        updateProgress: (ArrayList<DownloadInfo>) -> Unit

    ): Boolean {

        val query = DownloadManager.Query()
        query.setFilterById(*runningIds)
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING)


        ThreadUtils.postToBackgroundThread {
            var cursor: Cursor? = null
            try {
                cursor = downloadManager.query(query)
                if (cursor != null) {
                    val list = ArrayList<DownloadInfo>()
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                        val totalSize = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val currentSize =
                            cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val info = DownloadInfo()
                        info.downloadId = id
                        info.sizeTotal = totalSize
                        info.sizeSoFar = currentSize
                        list.add(info)
                    }
                    if (list.size > 0) {
                        ThreadUtils.postToMainThread {
                            updateProgress(list)
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                CursorUtils.closeCursorSafely(cursor)
            }
        }
        return true
    }
}
