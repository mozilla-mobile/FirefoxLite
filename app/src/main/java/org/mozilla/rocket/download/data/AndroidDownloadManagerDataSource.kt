package org.mozilla.rocket.download.data

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDownloadManagerDataSource(private val appContext: Context) {

    private val downloadManager by lazy { appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    suspend fun queryDownloadingItems(runningIds: LongArray): List<DownloadInfo> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query()
        query.setFilterById(*runningIds)
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING)
        downloadManager.query(query).use {
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
                return@withContext list
            }
        }
        return@withContext emptyList<DownloadInfo>()
    }

    suspend fun remove(downloadId: Long) = withContext(Dispatchers.IO) {
        downloadManager.remove(downloadId)
    }
}