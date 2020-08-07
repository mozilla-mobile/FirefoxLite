/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.download.data

import android.app.DownloadManager
import android.content.AsyncQueryHandler
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.provider.DownloadContract
import java.util.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by anlin on 17/08/2017.
 */
class DownloadInfoManager(private val appContext: Context) {

    private val queryHandler by lazy { DownloadInfoQueryHandler(appContext) }

    suspend fun enqueueDownload(downloadId: Long): Boolean = withContext(Dispatchers.IO) {
        val downloadInfo = DownloadInfo()
        downloadInfo.downloadId = downloadId

        // On Pixel, When downloading downloaded content which is still available, DownloadManager
        // returns the previous download id.
        // For that case we remove the old entry and re-insert a new one to move it to the top.
        // (Note that this is not the case for devices like Samsung, I have not verified yet if this
        // is a because of on those devices we move files to SDcard or if this is true even if the
        // file is not moved.)
        if (!hasDownloadItem(downloadId)) {
            val rowId = insert(downloadInfo)
            notifyRowUpdated(rowId)
            return@withContext true
        } else {
            val info = queryByDownloadId(downloadId)
            info?.rowId?.let { delete(it) }
            info?.let {
                val rowId = insert(it)
                notifyRowUpdated(rowId)
                relocateFileFinished(rowId)
            }
            return@withContext false
        }
    }

    private suspend fun insert(downloadInfo: DownloadInfo) = suspendCoroutine<Long> { continuation ->
        queryHandler.startInsert(
            TOKEN,
            object : AsyncInsertListener {
                override fun onInsertComplete(id: Long) {
                    continuation.resume(id)
                }
            },
            DownloadContract.Download.CONTENT_URI,
            getContentValuesFromDownloadInfo(downloadInfo)
        )
    }

    private fun getContentValuesFromDownloadInfo(downloadInfo: DownloadInfo): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(DownloadContract.Download.DOWNLOAD_ID, downloadInfo.downloadId)
        contentValues.put(DownloadContract.Download.FILE_PATH, downloadInfo.fileUri)
        contentValues.put(DownloadContract.Download.STATUS, downloadInfo.status)
        contentValues.put(DownloadContract.Download.IS_READ, downloadInfo.isRead)
        return contentValues
    }

    suspend fun delete(rowId: Long) = suspendCoroutine<Int> { continuation ->
        queryHandler.startDelete(
            TOKEN,
            AsyncDeleteWrapper(rowId, object : AsyncDeleteListener {
                override fun onDeleteComplete(result: Int, id: Long) {
                    continuation.resume(result)
                }
            }),
            DownloadContract.Download.CONTENT_URI,
            DownloadContract.Download._ID + " = ?",
            arrayOf(rowId.toString())
        )
    }

    suspend fun updateByRowId(downloadInfo: DownloadInfo) = suspendCoroutine<Int> { continuation ->
        queryHandler.startUpdate(
            TOKEN,
            object : AsyncUpdateListener {
                override fun onUpdateComplete(result: Int) {
                    continuation.resume(result)
                }
            },
            DownloadContract.Download.CONTENT_URI,
            getContentValuesFromDownloadInfo(downloadInfo),
            DownloadContract.Download._ID + " = ?",
            arrayOf(downloadInfo.rowId.toString())
        )
    }

    suspend fun query(offset: Int, limit: Int) = suspendCoroutine<List<DownloadInfo>> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit
        queryHandler.startQuery(
            TOKEN,
            object : AsyncQueryListener {
                override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                    continuation.resume(downloadInfoList)
                }
            },
            Uri.parse(uri),
            null,
            null,
            null,
            DownloadContract.Download._ID + " DESC"
        )
    }

    suspend fun queryByDownloadId(downloadId: Long) = suspendCoroutine<DownloadInfo?> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        queryHandler.startQuery(
            TOKEN,
            object : AsyncQueryListener {
                override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                    continuation.resume(
                        if (downloadInfoList.isNotEmpty()) {
                            downloadInfoList[0]
                        } else {
                            null
                        }
                    )
                }
            },
            Uri.parse(uri),
            null,
            DownloadContract.Download.DOWNLOAD_ID + "==?", arrayOf(downloadId.toString()),
            null
        )
    }

    suspend fun queryByRowId(rowId: Long) = suspendCoroutine<DownloadInfo?> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        queryHandler.startQuery(
            TOKEN,
            object : AsyncQueryListener {
                override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                    continuation.resume(
                        if (downloadInfoList.isNotEmpty()) {
                            downloadInfoList[0]
                        } else {
                            null
                        }
                    )
                }
            },
            Uri.parse(uri),
            null,
            DownloadContract.Download._ID + "==?",
            arrayOf(rowId.toString()),
            null
        )
    }

    suspend fun queryDownloadingAndUnreadIds() = suspendCoroutine<List<DownloadInfo>> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        queryHandler.startQuery(
            TOKEN,
            object : AsyncQueryListener {
                override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                    continuation.resume(downloadInfoList)
                }
            },
            Uri.parse(uri),
            null,
            DownloadContract.Download.STATUS + "!=? or " + DownloadContract.Download.IS_READ + "=?", arrayOf(STATUS_SUCCESSFUL, "0"),
            null
        )
    }

    suspend fun markAllItemsAreRead() = suspendCoroutine<Int> { continuation ->
        val contentValues = ContentValues()
        contentValues.put(DownloadContract.Download.IS_READ, "1")
        queryHandler.startUpdate(
            TOKEN,
            object : AsyncUpdateListener {
                override fun onUpdateComplete(result: Int) {
                    continuation.resume(result)
                }
            },
            DownloadContract.Download.CONTENT_URI, contentValues, DownloadContract.Download.STATUS + "=? and " + DownloadContract.Download.IS_READ + " = ?", arrayOf(STATUS_SUCCESSFUL, "0")
        )
    }

    fun hasDownloadItem(downloadId: Long): Boolean {
        val resolver = appContext.contentResolver
        val uri = DownloadContract.Download.CONTENT_URI
        val selection = DownloadContract.Download.DOWNLOAD_ID + "=" + downloadId
        resolver.query(uri, null, selection, null, null).use {
            return it != null && it.count > 0 && it.moveToFirst()
        }
    }

    fun notifyRowUpdated(rowId: Long) {
        val intent = Intent(DownloadInfo.ROW_UPDATED)
        intent.putExtra(DownloadInfo.ROW_ID, rowId)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
    }

    fun relocateFileFinished(rowId: Long) {
        RelocateService.broadcastRelocateFinished(appContext, rowId)
    }

    private class DownloadInfoQueryHandler(context: Context) : AsyncQueryHandler(context.contentResolver) {
        override fun onInsertComplete(token: Int, cookie: Any?, uri: Uri?) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    val id = uri?.lastPathSegment?.toLong() ?: -1
                    (cookie as AsyncInsertListener).onInsertComplete(id)
                }
            }
        }

        override fun onDeleteComplete(token: Int, cookie: Any?, result: Int) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    val wrapper = cookie as AsyncDeleteWrapper
                    wrapper.listener?.onDeleteComplete(result, wrapper.id)
                }
            }
        }

        override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    (cookie as AsyncUpdateListener).onUpdateComplete(result)
                }
            }
        }

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            val downloadInfoList: MutableList<DownloadInfo> = ArrayList()
            cursor?.use { safeCursor ->
                when (token) {
                    TOKEN -> while (cursor.moveToNext()) {
                        val downloadId = safeCursor.getLong(safeCursor.getColumnIndex(DownloadContract.Download.DOWNLOAD_ID))
                        val rowId = safeCursor.getLong(safeCursor.getColumnIndex(DownloadContract.Download._ID))
                        val fileUri = safeCursor.getString(safeCursor.getColumnIndex(DownloadContract.Download.FILE_PATH))
                        downloadInfoList.add(DownloadInfo.createEmptyDownloadInfo(downloadId, rowId, fileUri))
                    }
                }
            }
            if (cookie != null) {
                (cookie as AsyncQueryListener).onQueryComplete(downloadInfoList)
            }
        }
    }

    private class AsyncDeleteWrapper(var id: Long, var listener: AsyncDeleteListener?)

    interface AsyncInsertListener {
        fun onInsertComplete(id: Long)
    }

    interface AsyncDeleteListener {
        fun onDeleteComplete(result: Int, id: Long)
    }

    interface AsyncUpdateListener {
        fun onUpdateComplete(result: Int)
    }

    interface AsyncQueryListener {
        fun onQueryComplete(downloadInfoList: List<DownloadInfo>)
    }

    companion object {
        private const val TOKEN = 2
        private const val STATUS_SUCCESSFUL = DownloadManager.STATUS_SUCCESSFUL.toString()
    }
}
