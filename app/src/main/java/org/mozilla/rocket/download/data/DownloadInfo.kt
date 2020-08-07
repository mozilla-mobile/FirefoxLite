/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.download.data

import android.text.TextUtils
import java.util.Calendar
import java.util.Formatter
import java.util.Locale

/**
 * Created by anlin on 27/07/2017.
 */
class DownloadInfo {
    var rowId: Long? = null
    var downloadId: Long? = null
    var status = 0
        private set
    var reason = 0
    var size: String? = null
        private set
    var date: String? = null
        private set
    var fileName: String? = ""
        set(fileName) {
            if (!TextUtils.isEmpty(fileName)) {
                field = fileName
            }
        }
    var mediaUri: String? = ""
        set(mediaUri) {
            if (!TextUtils.isEmpty(mediaUri)) {
                field = mediaUri
            }
        }
    var mimeType: String? = ""
        set(mimeType) {
            if (!TextUtils.isEmpty(mimeType)) {
                field = mimeType
            }
        }
    var fileUri: String? = ""
        set(fileUri) {
            if (!TextUtils.isEmpty(fileUri)) {
                field = fileUri
            }
        }
    var fileExtension: String? = ""
        set(fileExtension) {
            if (!TextUtils.isEmpty(fileExtension)) {
                field = fileExtension
            }
        }
    var description: String? = ""
        set(description) {
            if (!TextUtils.isEmpty(description)) {
                field = description
            }
        }
    var sizeSoFar = 0.0
    var sizeTotal = 0.0
    var isRead = false

    fun setStatusInt(status: Int) {
        this.status = status
    }

    fun setSize(size: Double) {
        this.size = convertByteToReadable(size)
    }

    fun setDate(millis: Long) {
        date = convertMillis(millis)
    }

    private fun convertMillis(millis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return Formatter().format("%tB %td", calendar, calendar).toString()
    }

    private fun convertByteToReadable(bytes: Double): String {
        var displaySize = bytes
        val dictionary = arrayOf("bytes", "KB", "MB", "GB")
        var index = 0
        while (index < dictionary.size) {
            if (displaySize < 1024) {
                break
            }
            displaySize /= 1024
            index++
        }
        return String.format(Locale.getDefault(), "%.1f", displaySize) + dictionary[index]
    }

    fun existInDownloadManager(): Boolean {
        return status != STATUS_DELETED
    }

    companion object {
        // An overriding extra definition at the same level with DownloadManager.STATUS_SUCCESSFUL
        // for the status field of this class which is an alternative when the
        // DownloadManager.COLUMN_STATUS field is not available because no matching entry is available
        // in the DownloadManager's table.
        const val STATUS_DELETED = -1
        const val REASON_DEFAULT = -2

        fun createEmptyDownloadInfo(
            downloadId: Long,
            rowId: Long,
            fileUri: String,
            status: Int = STATUS_DELETED
        ): DownloadInfo {
            val info = DownloadInfo()
            info.rowId = rowId
            info.downloadId = downloadId
            info.fileUri = fileUri
            info.setStatusInt(status)
            return info
        }
    }
}