package org.mozilla.rocket.download.data

import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.tabs.web.Download

class DownloadInfoRepository(private val downloadManagerDataSource: AndroidDownloadManagerDataSource) {

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

    suspend fun queryByDownloadId(rowId: Long) =
        DownloadInfoManager.getInstance().queryByDownloadId(rowId)

    suspend fun queryDownloadingItems(runningIds: LongArray): List<DownloadInfo> =
        downloadManagerDataSource.queryDownloadingItems(runningIds)

    suspend fun markAllItemsAreRead() =
        DownloadInfoManager.getInstance().markAllItemsAreRead()

    suspend fun loadData(offset: Int, pageSize: Int) =
        DownloadInfoManager.getInstance().query(offset, pageSize)

    suspend fun remove(rowId: Long) =
        DownloadInfoManager.getInstance().delete(rowId)

    suspend fun enqueueToDownloadManager(download: Download, refererUrl: String?, shouldShowInDownloadList: Boolean = true): DownloadState {
        val result = downloadManagerDataSource.enqueue(download, refererUrl)
        if (shouldShowInDownloadList) {
            when (result) {
                is DownloadState.Success -> {
                    val newlyAdded = DownloadInfoManager.getInstance().enqueueDownload(result.downloadId)
                    if (newlyAdded) {
                        val headerInfo = downloadManagerDataSource.getDownloadUrlHeaderInfo(download.url)
                        val contentLengthFromDownloadRequest: Long = download.contentLength // it'll be -1 if it's from context menu, and real file size from webview callback
                        val fileSize = if (contentLengthFromDownloadRequest == -1L) headerInfo.contentLength else contentLengthFromDownloadRequest
                        TelemetryWrapper.startDownloadFile(result.downloadId.toString(), fileSize, headerInfo.isValidSSL, headerInfo.isSupportRange)
                    }
                }
            }
        }
        return result
    }

    suspend fun deleteFromDownloadManager(downloadId: Long) =
        downloadManagerDataSource.remove(downloadId)

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

    sealed class DownloadState {
        class Success(val downloadId: Long, val isStartFromContextMenu: Boolean) : DownloadState()
        object GeneralError : DownloadState()
        object StorageUnavailable : DownloadState()
        object FileNotSupported : DownloadState()
    }

    class HeaderInfo(
        val isSupportRange: Boolean = false,
        val isValidSSL: Boolean = true,
        val contentLength: Long = 0L
    )
}
