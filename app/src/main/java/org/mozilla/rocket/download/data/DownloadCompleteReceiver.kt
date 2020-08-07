/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.download.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import java.io.File
import javax.inject.Inject

class DownloadCompleteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadInfoRepository: DownloadInfoRepository

    override fun onReceive(context: Context, intent: Intent) {
        appComponent(context).inject(this)

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            downloadInfoRepository.queryByDownloadId(downloadId)?.let { downloadInfo ->
                // track the event when the file download completes successfully.
                if (downloadInfo.status == DownloadManager.STATUS_SUCCESSFUL) {
                    val progress = if (downloadInfo.sizeTotal != 0.0) {
                        downloadInfo.sizeSoFar.times(100) / downloadInfo.sizeTotal
                    } else {
                        0.0
                    }
                    TelemetryWrapper.endDownloadFile(
                        downloadId,
                        downloadInfo.sizeTotal.toLong(),
                        progress,
                        downloadInfo.status,
                        downloadInfo.reason
                    )
                }
            }
            val downloadInfo = downloadInfoRepository.queryByDownloadId(downloadId)
                ?: return@launch
            if (downloadInfo.status != DownloadManager.STATUS_SUCCESSFUL) {
                // track the event when the file download cancel from notification tray.
                TelemetryWrapper.endDownloadFile(
                    downloadId,
                    null,
                    null,
                    DownloadInfo.STATUS_DELETED,
                    DownloadInfo.REASON_DEFAULT
                )
            }
            if (downloadInfo.status == DownloadManager.STATUS_SUCCESSFUL && !TextUtils.isEmpty(downloadInfo.fileUri)) {
                // have to update, then the fileUri may write into our DB.
                downloadInfoRepository.updateByRowId(downloadInfo)
                startRelocationService(context, downloadInfo)
            }
            // Download canceled
            if (!downloadInfo.existInDownloadManager()) {
                downloadInfo.rowId?.let { downloadInfoRepository.remove(it) }
            }
        }
    }

    private suspend fun startRelocationService(context: Context, downloadInfo: DownloadInfo) = withContext(Dispatchers.IO) {
        val fileUri = Uri.parse(downloadInfo.fileUri)
        if ("file" == fileUri.scheme) {
            // on some device the uri is "file:///storage/emulated/0/Download/file.png"
            // but the real path is "file:///storage/emulated/legacy/Download/file.png"
            // Since we already restrict download folder when we were making request to
            // DownloadManager, now we only look for the file-name in download folder.
            val fileName = File(fileUri.path).name
            val type = Environment.DIRECTORY_DOWNLOADS
            val dir = Environment.getExternalStoragePublicDirectory(type)
            val downloadedFile = File(dir, fileName)
            val rowId = downloadInfo.rowId ?: -1L
            val downloadId = downloadInfo.downloadId ?: -1L
            if (rowId != -1L && downloadId != -1L && downloadedFile.exists() && downloadedFile.canWrite()) {
                RelocateService.startActionMove(
                    context,
                    rowId,
                    downloadId,
                    downloadedFile,
                    downloadInfo.mimeType
                )
            }
        }
    }
}
