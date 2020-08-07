package org.mozilla.rocket.download.data

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.network.SocketTags
import org.mozilla.rocket.tabs.web.Download
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.net.ssl.SSLHandshakeException

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

    fun addCompletedDownload(
        title: String,
        description: String,
        isMediaScannerScannable: Boolean,
        mimeType: String,
        path: String,
        length: Long,
        showNotification: Boolean
    ) = downloadManager.addCompletedDownload(title, description, isMediaScannerScannable, mimeType, path, length, showNotification)

    suspend fun getDownloadUrlHeaderInfo(url: String): DownloadInfoRepository.HeaderInfo = withContext(Dispatchers.IO) {
        TrafficStats.setThreadStatsTag(SocketTags.DOWNLOADS)
        var connection: HttpURLConnection? = null
        var isSupportRange = false
        var isValidSSL = true
        var contentLength = 0L
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            val headerField = connection.getHeaderField("Accept-Ranges")
            isSupportRange = (headerField != null && headerField == "bytes")
            val strContentLength = connection.getHeaderField("Content-Length")
            contentLength = strContentLength?.toLong() ?: 0L
            connection.responseCode
            connection.disconnect()
        } catch (e: SSLHandshakeException) {
            isValidSSL = false
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return@withContext DownloadInfoRepository.HeaderInfo(isSupportRange, isValidSSL, contentLength)
    }

    suspend fun queryDownloadItem(downloadId: Long): DownloadInfo? = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val downloadInfo = DownloadInfo()
                downloadInfo.downloadId = downloadId
                downloadInfo.description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))
                downloadInfo.setStatusInt(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
                downloadInfo.reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                downloadInfo.setSize(cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toDouble())
                downloadInfo.sizeTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toDouble()
                downloadInfo.sizeSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).toDouble()
                downloadInfo.setDate(cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)))
                downloadInfo.mediaUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI))
                downloadInfo.fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                if (downloadInfo.fileUri != null) {
                    val extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(downloadInfo.fileUri, "UTF-8"))
                    downloadInfo.mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT))
                    downloadInfo.fileExtension = extension
                    downloadInfo.fileName = File(Uri.parse(downloadInfo.fileUri).path).name
                }
                return@withContext downloadInfo
            }
        }
        return@withContext null
    }

    suspend fun queryDownloadingItems(runningIds: LongArray): List<DownloadInfo> = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query()
        query.setFilterById(*runningIds)
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING)
        downloadManager.query(query)?.use { cursor ->
            val list = ArrayList<DownloadInfo>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                val totalSize = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val currentSize = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val info = DownloadInfo()
                info.downloadId = id
                info.sizeTotal = totalSize
                info.sizeSoFar = currentSize
                list.add(info)
            }
            return@withContext list
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