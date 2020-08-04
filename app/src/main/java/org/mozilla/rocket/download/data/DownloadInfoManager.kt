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
import android.text.TextUtils
import android.view.View
import android.webkit.MimeTypeMap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.focus.provider.DownloadContract
import org.mozilla.focus.utils.CursorUtils
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.rocket.util.LoggerWrapper
import org.mozilla.threadutils.ThreadUtils
import java.io.File
import java.net.URISyntaxException
import java.net.URLEncoder
import java.util.ArrayList
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by anlin on 17/08/2017.
 */
class DownloadInfoManager {

    suspend fun enqueueDownload(downloadId: Long): Boolean = withContext(Dispatchers.IO) {
        val downloadInfo = DownloadInfo()
        downloadInfo.downloadId = downloadId

        // On Pixel, When downloading downloaded content which is still available, DownloadManager
        // returns the previous download id.
        // For that case we remove the old entry and re-insert a new one to move it to the top.
        // (Note that this is not the case for devices like Samsung, I have not verified yet if this
        // is a because of on those devices we move files to SDcard or if this is true even if the
        // file is not moved.)
        if (!recordExists(downloadId)) {
            val rowId = insert(downloadInfo)
            notifyRowUpdated(mContext, rowId)
            return@withContext true
        } else {
            val info = queryByDownloadId(downloadId)
            info?.rowId?.let { delete(it) }
            info?.let {
                val rowId = insert(it)
                notifyRowUpdated(mContext, rowId)
                RelocateService.broadcastRelocateFinished(mContext, rowId)
            }
            return@withContext false
        }
    }

    private suspend fun insert(downloadInfo: DownloadInfo) = suspendCoroutine<Long> { continuation ->
        mQueryHandler.startInsert(
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

    suspend fun delete(rowId: Long) = suspendCoroutine<Int> { continuation ->
        mQueryHandler.startDelete(
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

    fun updateByRowId(downloadInfo: DownloadInfo, listener: AsyncUpdateListener?) {
        mQueryHandler.startUpdate(TOKEN, listener, DownloadContract.Download.CONTENT_URI, getContentValuesFromDownloadInfo(downloadInfo), DownloadContract.Download._ID + " = ?", arrayOf(downloadInfo.rowId.toString()))
    }

    suspend fun query(offset: Int, limit: Int) = suspendCoroutine<List<DownloadInfo>> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString() + "?offset=" + offset + "&limit=" + limit
        mQueryHandler.startQuery(
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

    fun queryByDownloadId(downloadId: Long, listener: AsyncQueryListener?) {
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        mQueryHandler.startQuery(TOKEN, listener, Uri.parse(uri), null, DownloadContract.Download.DOWNLOAD_ID + "==?", arrayOf(downloadId.toString()), null)
    }

    private suspend fun queryByDownloadId(downloadId: Long) = suspendCoroutine<DownloadInfo?> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        mQueryHandler.startQuery(
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

    fun queryByRowId(rowId: Long, listener: AsyncQueryListener?) {
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        mQueryHandler.startQuery(TOKEN, listener, Uri.parse(uri), null, DownloadContract.Download._ID + "==?", arrayOf(rowId.toString()), null)
    }

    suspend fun queryDownloadingAndUnreadIds() = suspendCoroutine<List<DownloadInfo>> { continuation ->
        val uri = DownloadContract.Download.CONTENT_URI.toString()
        mQueryHandler.startQuery(
            TOKEN,
            object : AsyncQueryListener {
                override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                    continuation.resume(downloadInfoList)
                }
            },
            Uri.parse(uri),
            null,
            DownloadContract.Download.STATUS + "!=? or " + DownloadContract.Download.IS_READ + "=?", arrayOf(DownloadManager.STATUS_SUCCESSFUL.toString(), "0"),
            null
        )
    }

    suspend fun markAllItemsAreRead() = suspendCoroutine<Int> { continuation ->
        val contentValues = ContentValues()
        contentValues.put(DownloadContract.Download.IS_READ, "1")
        mQueryHandler.startUpdate(
            TOKEN,
            object : AsyncUpdateListener {
                override fun onUpdateComplete(result: Int) {
                    continuation.resume(result)
                }
            },
            DownloadContract.Download.CONTENT_URI, contentValues, DownloadContract.Download.STATUS + "=? and " + DownloadContract.Download.IS_READ + " = ?", arrayOf(DownloadManager.STATUS_SUCCESSFUL.toString(), "0")
        )
    }

    fun recordExists(downloadId: Long): Boolean {
        val resolver = mContext.contentResolver
        val uri = DownloadContract.Download.CONTENT_URI
        val selection = DownloadContract.Download.DOWNLOAD_ID + "=" + downloadId
        resolver.query(uri, null, selection, null, null).use {
            return it != null && it.count > 0 && it.moveToFirst()
        }
    }

    /**
     * Update database, to replace file path of a record in both of our own db and DownloadManager.
     *
     * @param downloadId download id for record in DownloadManager
     * @param newPath new file path
     * @param type Mime type
     */
    fun replacePath(downloadId: Long, newPath: String, type: String) {
        val newFile = File(newPath)
        val pojo = queryDownloadManager(mContext, downloadId)
        if (pojo == null) {
            // Should never happen
            val msg = "File entry disappeared after being moved"
            throw IllegalStateException(msg)
        }

        // remove old download from DownloadManager, then add new one
        // Description and MIME cannot be blank, otherwise system refuse to add new record
        val manager = mContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val desc = if (TextUtils.isEmpty(pojo.desc)) "Downloaded from internet" else pojo.desc
        val mimeType = if (TextUtils.isEmpty(pojo.mime)) if (TextUtils.isEmpty(type)) "*/*" else type else pojo.mime
        val visible = true // otherwise we need permission DOWNLOAD_WITHOUT_NOTIFICATION
        val newId = manager.addCompletedDownload(
            newFile.name,
            desc,
            true,
            mimeType,
            newPath,
            newFile.length(),
            visible)

        // filename might be different from old file
        // update by row id
        queryByDownloadId(downloadId, object : AsyncQueryListener {
            override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                for (i in downloadInfoList.indices) {
                    val queryDownloadInfo = downloadInfoList[i]
                    if (!queryDownloadInfo.existInDownloadManager()) {
                        // Should never happen
                        val msg = "File entry disappeared after being moved"
                        throw IllegalStateException(msg)
                    }
                    if (downloadId == queryDownloadInfo.downloadId) {
                        queryDownloadInfo.rowId?.let {
                            val newInfo = pojoToDownloadInfo(pojo, newPath, it)
                            newInfo.downloadId = newId
                            updateByRowId(newInfo, object : AsyncUpdateListener {
                                override fun onUpdateComplete(result: Int) {
                                    notifyRowUpdated(mContext, it)
                                    RelocateService.broadcastRelocateFinished(mContext, it)
                                }
                            })
                            manager.remove(downloadId)
                        }
                        break
                    }
                }
            }
        })
    }

    fun showOpenDownloadSnackBar(rowId: Long, container: View, logTag: String?) {
        queryByRowId(rowId, object : AsyncQueryListener {
            override fun onQueryComplete(downloadInfoList: List<DownloadInfo>) {
                val existInLocalDB = downloadInfoList.isNotEmpty()
                if (!existInLocalDB) {
                    LoggerWrapper.throwOrWarn(logTag, "Download Completed with unknown local row id")
                    return
                }
                val downloadInfo = downloadInfoList[0]
                val existInDownloadManager = downloadInfo.existInDownloadManager()
                if (!existInDownloadManager) {
                    LoggerWrapper.throwOrWarn(logTag, "Download Completed with unknown DownloadManager id")
                }
                val completedStr = container.context.getString(R.string.download_completed, downloadInfo.fileName)
                val snackbar = Snackbar.make(container, completedStr, Snackbar.LENGTH_LONG)
                // Set the open action only if we can.
                if (existInDownloadManager) {
                    snackbar.setAction(R.string.open) {
                        try {
                            IntentUtils.intentOpenFile(container.context, downloadInfo.fileUri, downloadInfo.mimeType)
                        } catch (e: URISyntaxException) {
                            e.printStackTrace()
                        }
                    }
                }
                snackbar.show()
            }
        })
    }

    fun queryDownloadManager(downloadId: Long): DownloadPojo? {
        return queryDownloadManager(mContext, downloadId)
    }

    private class DownloadInfoQueryHandler(context: Context) : AsyncQueryHandler(context.contentResolver) {
        override fun onInsertComplete(token: Int, cookie: Any?, uri: Uri?) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    val id = uri?.lastPathSegment?.toLong() ?: -1
                    (cookie as AsyncInsertListener).onInsertComplete(id)
                }
                else -> {
                }
            }
        }

        override fun onDeleteComplete(token: Int, cookie: Any?, result: Int) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    val wrapper = cookie as AsyncDeleteWrapper
                    wrapper.listener?.onDeleteComplete(result, wrapper.id)
                }
                else -> {
                }
            }
        }

        override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
            when (token) {
                TOKEN -> if (cookie != null) {
                    (cookie as AsyncUpdateListener).onUpdateComplete(result)
                }
                else -> {
                }
            }
        }

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            when (token) {
                TOKEN -> ThreadUtils.postToBackgroundThread {
                    if (cookie != null) {
                        val downloadInfoList: MutableList<DownloadInfo> = ArrayList()
                        if (cursor != null) {
                            try {
                                while (cursor.moveToNext()) {
                                    val downloadInfo = cursorToDownloadInfo(cursor)
                                    downloadInfoList.add(downloadInfo)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                CursorUtils.closeCursorSafely(cursor)
                            }
                        }
                        ThreadUtils.postToMainThread { (cookie as AsyncQueryListener).onQueryComplete(downloadInfoList) }
                    } else {
                        CursorUtils.closeCursorSafely(cursor)
                    }
                }
                else -> CursorUtils.closeCursorSafely(cursor)
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

    /* Data class to store queried information from DownloadManager */
    class DownloadPojo {
        var downloadId: Long = 0
        var desc: String? = null
        var mime: String? = null

        @JvmField
        var length: Long = 0

        @JvmField
        var sizeSoFar: Long = 0

        @JvmField
        var status = 0

        @JvmField
        var reason = 0
        var timeStamp: Long = 0
        var mediaUri: String? = null
        var fileUri: String? = null
        var fileExtension: String? = null
        var fileName: String? = null
    }

    companion object {
        const val ROW_ID = "row id"
        const val ROW_UPDATED = "row_updated"
        private const val TOKEN = 2
        private var sInstance: DownloadInfoManager? = null
        private lateinit var mContext: Context
        private lateinit var mQueryHandler: DownloadInfoQueryHandler

        @JvmStatic
        fun getInstance(): DownloadInfoManager {
            if (sInstance == null) {
                sInstance = DownloadInfoManager()
            }
            return sInstance!!
        }

        fun init(context: Context) {
            mContext = context
            mQueryHandler = DownloadInfoQueryHandler(context)
        }

        @JvmStatic
        fun notifyRowUpdated(context: Context, rowId: Long) {
            val intent = Intent(ROW_UPDATED)
            intent.putExtra(ROW_ID, rowId)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        private fun getContentValuesFromDownloadInfo(downloadInfo: DownloadInfo): ContentValues {
            val contentValues = ContentValues()
            contentValues.put(DownloadContract.Download.DOWNLOAD_ID, downloadInfo.downloadId)
            contentValues.put(DownloadContract.Download.FILE_PATH, downloadInfo.fileUri)
            contentValues.put(DownloadContract.Download.STATUS, downloadInfo.status)
            contentValues.put(DownloadContract.Download.IS_READ, downloadInfo.isRead)
            return contentValues
        }

        private fun cursorToDownloadInfo(cursor: Cursor): DownloadInfo {
            val downloadId = cursor.getLong(cursor.getColumnIndex(DownloadContract.Download.DOWNLOAD_ID))
            val fileUri = cursor.getString(cursor.getColumnIndex(DownloadContract.Download.FILE_PATH))
            val rowId = cursor.getLong(cursor.getColumnIndex(DownloadContract.Download._ID))
            val pojo = queryDownloadManager(mContext, downloadId)
            return pojo?.let { pojoToDownloadInfo(it, fileUri, rowId) }
                ?: createEmptyDownloadInfo(downloadId, rowId, fileUri)
        }

        private fun pojoToDownloadInfo(pojo: DownloadPojo, fileUri: String, rowId: Long): DownloadInfo {
            val info = DownloadInfo()
            info.rowId = rowId
            info.fileName = pojo.fileName
            info.downloadId = pojo.downloadId
            info.setSize(pojo.length.toDouble())
            info.sizeTotal = pojo.length.toDouble()
            info.sizeSoFar = pojo.sizeSoFar.toDouble()
            info.setStatusInt(pojo.status)
            info.reason = pojo.reason
            info.setDate(pojo.timeStamp)
            info.mediaUri = pojo.mediaUri
            info.fileUri = pojo.fileUri
            info.mimeType = pojo.mime
            info.fileExtension = pojo.fileExtension
            if (TextUtils.isEmpty(pojo.fileUri)) {
                info.fileUri = fileUri
            } else {
                info.fileUri = pojo.fileUri
            }
            return info
        }

        private fun createEmptyDownloadInfo(downloadId: Long, rowId: Long, fileUri: String): DownloadInfo {
            val info = DownloadInfo()
            info.rowId = rowId
            info.downloadId = downloadId
            info.fileUri = fileUri
            info.setStatusInt(DownloadInfo.STATUS_DELETED)
            info.setStatusInt(DownloadInfo.REASON_DEFAULT)
            return info
        }

        private fun queryDownloadManager(context: Context, downloadId: Long): DownloadPojo? {
            // query download manager
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val managerCursor = manager.query(query)
            val pojo = DownloadPojo()
            pojo.downloadId = downloadId
            try {
                if (managerCursor != null && managerCursor.moveToFirst()) {
                    pojo.desc = managerCursor.getString(managerCursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))
                    pojo.status = managerCursor.getInt(managerCursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    pojo.reason = managerCursor.getInt(managerCursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                    pojo.length = managerCursor.getLong(managerCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    pojo.sizeSoFar = managerCursor.getLong(managerCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    pojo.timeStamp = managerCursor.getLong(managerCursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                    pojo.mediaUri = managerCursor.getString(managerCursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI))
                    pojo.fileUri = managerCursor.getString(managerCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                    if (pojo.fileUri != null) {
                        val extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(pojo.fileUri, "UTF-8"))
                        pojo.mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT))
                        pojo.fileExtension = extension
                        pojo.fileName = File(Uri.parse(pojo.fileUri).path).name
                    }
                } else {
                    // No pojo
                    return null
                }
            } catch (e: Exception) {
                // No valid pojo
                return null
            } finally {
                CursorUtils.closeCursorSafely(managerCursor)
            }
            return pojo
        }
    }
}