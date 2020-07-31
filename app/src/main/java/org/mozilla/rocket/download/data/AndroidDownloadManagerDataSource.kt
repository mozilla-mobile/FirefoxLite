package org.mozilla.rocket.download.data

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.rocket.tabs.web.Download

class AndroidDownloadManagerDataSource(private val appContext: Context) {

    private val downloadManager by lazy { appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    suspend fun enqueue(download: Download, refererUrl: String?): DownloadInfoRepository.DownloadState = withContext(Dispatchers.IO) {
        val cookie = CookieManager.getInstance().getCookie(download.url)
        val fileName = download.name
            ?: URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType)

        // so far each download always return null even for an image.
        // But we might move downloaded file to another directory.
        // So, for now we always save file to DIRECTORY_DOWNLOADS
        val dir = Environment.DIRECTORY_DOWNLOADS

        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
            return@withContext DownloadInfoRepository.DownloadState.StorageUnavailable
        }

        // block non-http/https download links
        if (!URLUtil.isNetworkUrl(download.url)) {
            return@withContext DownloadInfoRepository.DownloadState.FileNotSupported
        }

        if (!isDownloadManagerEnabled(appContext)) {
            return@withContext DownloadInfoRepository.DownloadState.GeneralError
        }

        val request = DownloadManager.Request(Uri.parse(download.url))
            .addRequestHeader("User-Agent", download.userAgent)
            .addRequestHeader("Cookie", cookie)
            .addRequestHeader("Referer", refererUrl)
            .setDestinationInExternalPublicDir(dir, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(download.mimeType)
            .also { it.allowScanningByMediaScanner() }

        val downloadId = downloadManager.enqueue(request)
        return@withContext DownloadInfoRepository.DownloadState.Success(downloadId, download.isStartFromContextMenu)
    }

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

    private fun isDownloadManagerEnabled(context: Context): Boolean {
        var state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        try {
            state = context.packageManager.getApplicationEnabledSetting(DOWNLOAD_MANAGER_PACKAGE_NAME)
        } catch (e: IllegalArgumentException) { // throws when the named package does not exist
            e.printStackTrace()
        }
        return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
    }

    companion object {
        private const val DOWNLOAD_MANAGER_PACKAGE_NAME = "com.android.providers.downloads"
    }
}