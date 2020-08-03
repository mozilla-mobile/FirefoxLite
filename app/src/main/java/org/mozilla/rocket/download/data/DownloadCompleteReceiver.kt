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
import org.mozilla.focus.components.RelocateService
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.threadutils.ThreadUtils
import java.io.File

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) {
            return
        }
        val downloadPojo = DownloadInfoManager.getInstance().queryDownloadManager(downloadId)
        // track the event when the file download completes successfully.
        if (downloadPojo != null && downloadPojo.status == DownloadManager.STATUS_SUCCESSFUL) {
            val progress = if (downloadPojo.length.toDouble() != 0.0) downloadPojo.sizeSoFar * 100.0 / downloadPojo.length else 0.0
            TelemetryWrapper.endDownloadFile(downloadId,
                downloadPojo.length,
                progress,
                downloadPojo.status,
                downloadPojo.reason)
        }
        DownloadInfoManager.getInstance().queryByDownloadId(downloadId, object : DownloadInfoManager.AsyncQueryListener {
            override fun onQueryComplete(downloadInfoList: List<*>) {
                if (downloadInfoList.size > 0) {
                    val downloadInfo = downloadInfoList[0] as DownloadInfo
                    if (downloadInfo.status != DownloadManager.STATUS_SUCCESSFUL) {
                        // track the event when the file download cancel from notification tray.
                        TelemetryWrapper.endDownloadFile(downloadId,
                            null,
                            null,
                            DownloadInfo.STATUS_DELETED,
                            DownloadInfo.REASON_DEFAULT)
                    }
                    if (downloadInfo.status == DownloadManager.STATUS_SUCCESSFUL
                        && !TextUtils.isEmpty(downloadInfo.fileUri)) {

                        // have to update, then the fileUri may write into our DB.
                        DownloadInfoManager.getInstance().updateByRowId(downloadInfo, object : DownloadInfoManager.AsyncUpdateListener {
                            override fun onUpdateComplete(result: Int) {
                                val fileUri = Uri.parse(downloadInfo.fileUri)
                                if ("file" == fileUri.scheme) {
                                    ThreadUtils.postToBackgroundThread { // on some device the uri is "file:///storage/emulated/0/Download/file.png"
                                        // but the real path is "file:///storage/emulated/legacy/Download/file.png"
                                        // Since we already restrict download folder when we were making request to
                                        // DownloadManager, now we only look for the file-name in download folder.
                                        val fileName = File(fileUri.path).name
                                        val type = Environment.DIRECTORY_DOWNLOADS
                                        val dir = Environment.getExternalStoragePublicDirectory(type)
                                        val downloadedFile = File(dir, fileName)
                                        if (downloadedFile.exists() && downloadedFile.canWrite()) {
                                            RelocateService.startActionMove(context,
                                                downloadInfo.rowId!!,
                                                downloadInfo.downloadId!!,
                                                downloadedFile,
                                                downloadInfo.mimeType)
                                        }
                                    }
                                }
                            }
                        })
                    }
                    // Download canceled
                    if (!downloadInfo.existInDownloadManager()) {
                        DownloadInfoManager.getInstance().delete(downloadInfo.rowId!!, null)
                    }
                }
            }
        })
    }
}