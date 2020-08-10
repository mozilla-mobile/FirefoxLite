package org.mozilla.rocket.download.data

import android.text.TextUtils
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.tabs.web.Download
import java.io.File

class DownloadsRepository(
    private val downloadManagerDataSource: AndroidDownloadManagerDataSource,
    private val downloadsLocalDataSource: DownloadsLocalDataSource
) {

    suspend fun enqueue(download: Download, refererUrl: String?, shouldShowInDownloadList: Boolean = true): DownloadState {
        val result = downloadManagerDataSource.enqueue(download, refererUrl)
        if (shouldShowInDownloadList) {
            when (result) {
                is DownloadState.Success -> {
                    val newlyAdded = downloadsLocalDataSource.enqueue(result.downloadId)
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

    suspend fun getDownload(downloadId: Long) =
        downloadsLocalDataSource.getDownload(downloadId)?.joinWithDownloadManagerResult()

    suspend fun getDownloadByRowId(rowId: Long) =
        downloadsLocalDataSource.getDownloadByRowId(rowId)?.joinWithDownloadManagerResult()

    fun hasDownloadItem(downloadId: Long) =
        downloadsLocalDataSource.hasDownloadItem(downloadId)

    suspend fun getDownloads(offset: Int, pageSize: Int) =
        downloadsLocalDataSource.getDownloads(offset, pageSize).mapNotNull { downloadInfo ->
            downloadInfo.joinWithDownloadManagerResult()
        }

    suspend fun getDownloadingItems(runningIds: LongArray): List<DownloadInfo> =
        downloadManagerDataSource.getDownloadingItems(runningIds)

    suspend fun getIndicatorStatus(): List<DownloadInfo> =
        downloadsLocalDataSource.getDownloadingAndUnreadIds().mapNotNull { downloadInfo ->
            downloadInfo.joinWithDownloadManagerResult()
        }

    suspend fun markAllItemsAreRead() =
        downloadsLocalDataSource.markAllItemsAreRead()

    suspend fun updateDownloadByRowId(downloadInfo: DownloadInfo) =
        downloadsLocalDataSource.updateDownloadByRowId(downloadInfo)

    suspend fun remove(rowId: Long) =
        downloadsLocalDataSource.remove(rowId)

    suspend fun delete(downloadId: Long) =
        downloadManagerDataSource.delete(downloadId)

    suspend fun replaceFilePath(downloadId: Long, newPath: String, type: String?) {
        val newFile = File(newPath)
        getDownload(downloadId)?.let { downloadInfo ->
            // remove old download from DownloadManager, then add new one
            // Description and MIME cannot be blank, otherwise system refuse to add new record
            val desc: String = downloadInfo.description?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_DOWNLOAD_DESCRIPTION

            val mimeType: String = downloadInfo.mimeType?.takeIf { it.isNotEmpty() }
                ?: (type?.takeIf { it.isNotEmpty() } ?: DEFAULT_DOWNLOAD_MIME_TYPE)

            val newId = downloadManagerDataSource.addCompletedDownload(
                newFile.name,
                desc,
                true,
                mimeType,
                newPath,
                newFile.length(),
                showNotification = true // otherwise we need permission DOWNLOAD_WITHOUT_NOTIFICATION
            )

            // filename might be different from old file
            // update by row id
            if (downloadInfo.existInDownloadManager()) {
                downloadInfo.rowId?.let {
                    val newDownloadInfo = DownloadInfo.createEmptyDownloadInfo(newId, it, newPath, downloadInfo.status)
                    downloadsLocalDataSource.updateDownloadByRowId(newDownloadInfo)
                    downloadManagerDataSource.delete(downloadId)
                    downloadsLocalDataSource.notifyRowUpdated(it)
                    downloadsLocalDataSource.relocateFileFinished(it)
                }
            }
        }
    }

    suspend fun trackDownloadCancel(downloadId: Long) {
        val downloadInfo = downloadManagerDataSource.getDownload(downloadId) ?: return
        val progress = if (downloadInfo.sizeTotal == 0.0) {
            0.0
        } else {
            downloadInfo.sizeSoFar.times(100) / downloadInfo.sizeTotal
        }
        // track the event when the file download cancel from Download Panel.
        TelemetryWrapper.endDownloadFile(
            downloadId,
            downloadInfo.sizeTotal.toLong(),
            progress,
            DownloadInfo.STATUS_DELETED,
            DownloadInfo.REASON_DEFAULT
        )
    }

    private suspend fun DownloadInfo.joinWithDownloadManagerResult(): DownloadInfo? {
        return downloadId?.let {
            downloadManagerDataSource.getDownload(it)?.also { info ->
                info.rowId = rowId
                if (TextUtils.isEmpty(info.fileUri)) {
                    info.fileUri = fileUri
                }
            }
        } ?: this
    }

    companion object {
        private const val DEFAULT_DOWNLOAD_DESCRIPTION = "Downloaded from internet"
        private const val DEFAULT_DOWNLOAD_MIME_TYPE = "*/*"
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
