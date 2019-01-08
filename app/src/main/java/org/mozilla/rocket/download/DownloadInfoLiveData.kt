package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.CursorUtils
import org.mozilla.threadutils.ThreadUtils

class DownloadInfoLiveData() : LiveData<DownloadInfoBundle>() {

    private val downloadInfoBundle: DownloadInfoBundle = DownloadInfoBundle(ArrayList(), -1, -1)

    private var handler: Handler? = null
    private var itemCount = 0
    var isOpening = false
    private var isLoading = false
    private var isLastPage = false
    private lateinit var context: Context

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
                if (id > 0) {
                    DownloadInfoManager.getInstance().queryByDownloadId(id, updateListener)
                }
            } else if (intent.action == DownloadInfoManager.ROW_UPDATED) {
                val id = intent.getLongExtra(DownloadInfoManager.ROW_ID, 0L)
                if (id > 0) {
                    DownloadInfoManager.getInstance().queryByRowId(id, updateListener)
                }
            }
        }
    }

    var updateListener = { downloadInfoList: List<DownloadInfo> ->
        for (downloadInfo in downloadInfoList) {
            updateItem(downloadInfo)
        }
    }

    private val runningDownloadIds: LongArray
        get() {
            val ids = downloadInfoBundle.list
                    .filter { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }
                    .map { it.downloadId }
            val array = LongArray(ids.size)
            for (i in array.indices) {
                array[i] = ids[i]
            }

            return array
        }

    private val isDownloading: Boolean
        get() {
            return downloadInfoBundle.list.any { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }
        }

    constructor(ctx: Context) : this() {
        context = ctx.applicationContext
    }

    override fun onActive() {
        super.onActive()
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, IntentFilter(DownloadInfoManager.ROW_UPDATED))
        context.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        val runningIds = runningDownloadIds
        if (!runningIds.isEmpty()) {
            updateRunningItems()
        }
    }

    override fun onInactive() {
        super.onInactive()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        context.unregisterReceiver(broadcastReceiver)
        cleanUp()
    }

    fun loadMore(init: Boolean) {
        if (init) {
            isLoading = false
            isLastPage = false
            isOpening = false
            itemCount = 0
            downloadInfoBundle.list.clear()
        }
        if (isLastPage || isLoading) {
            return
        }
        DownloadInfoManager.getInstance().query(itemCount, PAGE_SIZE) { downloadInfoList ->
            downloadInfoBundle.list.addAll(downloadInfoList)
            downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
            itemCount = downloadInfoBundle.list.size
            value = downloadInfoBundle
            isOpening = false
            isLoading = false
            isLastPage = downloadInfoList.size == 0
            if (isDownloading && handler == null) {
                handler = Handler()
                handler?.post(QueryProgressRunnable())
            }
        }
        isLoading = true
    }

    private fun updateRunningItems() {
        for (i in runningDownloadIds.indices) {
            DownloadInfoManager.getInstance().queryByDownloadId(runningDownloadIds[i], updateListener)
        }
    }

    private fun updateItem(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoBundle.list.size) {
            if (downloadInfoBundle.list[i].rowId == downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            downloadInfoBundle.list.add(0, downloadInfo)
        } else {
            downloadInfoBundle.list.removeAt(index)
            downloadInfoBundle.list.add(index, downloadInfo)
        }
        downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
        value = downloadInfoBundle
    }

    fun removeItem(rowId: Long) {
        DownloadInfoManager.getInstance().delete(rowId, null)
        hideItem(rowId)
    }

    fun delete(rowId: Long, listener: DownloadInfoViewModel.OnOperationComplete?) {
        DownloadInfoManager.getInstance().queryByRowId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0 && rowId == (downloadInfoList[0] as DownloadInfo).rowId) {

                val deletedDownload = downloadInfoList[0] as DownloadInfo
                listener?.onComplete(deletedDownload)
            }
        }
    }

    fun cancel(rowId: Long, listener: DownloadInfoViewModel.OnOperationComplete?) {

        DownloadInfoManager.getInstance().queryByRowId(rowId) { downloadInfoList ->
            if (downloadInfoList.size > 0) {
                val downloadInfo = downloadInfoList[0]

                if (downloadInfo.existInDownloadManager()) {
                    if (rowId == downloadInfo.rowId && DownloadManager.STATUS_SUCCESSFUL != downloadInfo.status) {

                        listener?.onComplete(downloadInfo)

                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.remove(downloadInfo.downloadId)

                        removeItem(rowId)
                    }
                }
            }
        }
    }

    fun addItem(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoBundle.list.size) {
            if (downloadInfoBundle.list[i].rowId < downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            downloadInfoBundle.list.add(downloadInfo)
            //The crash will happen when data set size is 1 after add item.
            //Because we define item count is 1 and mDownloadInfo is empty that means nothing and show empty view.
            //So use notifyDataSetChanged() instead of notifyItemInserted when data size is 1 after add item.
            if (downloadInfoBundle.list.size > 1) {
                downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED
                downloadInfoBundle.index = (downloadInfoBundle.list.size - 1).toLong()
            } else {
                downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
            }
        } else {
            downloadInfoBundle.list.add(index, downloadInfo)
            downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED
            downloadInfoBundle.index = index.toLong()
        }
        value = downloadInfoBundle
    }

    fun hideItem(rowId: Long) {
        for (i in 0 until downloadInfoBundle.list.size) {
            val downloadInfo = downloadInfoBundle.list[i]
            if (rowId == downloadInfo.rowId) {
                downloadInfoBundle.list.remove(downloadInfo)
                downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_REMOVED
                downloadInfoBundle.index = i.toLong()
                value = downloadInfoBundle
                break
            }
        }
    }

    private inner class QueryProgressRunnable : Runnable {
        override fun run() {
            if (queryDownloadProgress()) {
                handler?.postDelayed(this, QUERY_PROGRESS_DELAY)
            }
        }
    }

    private fun queryDownloadProgress(): Boolean {
        val query = DownloadManager.Query()
        val runningIds = runningDownloadIds
        if (runningIds.isEmpty()) {
            cleanUp()
            return false
        }
        query.setFilterById(*runningIds)
        query.setFilterByStatus(DownloadManager.STATUS_RUNNING)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        ThreadUtils.postToBackgroundThread {
            var cursor: Cursor? = null
            try {
                cursor = manager.query(query)
                if (cursor != null) {
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
                    if (list.size > 0) {
                        ThreadUtils.postToMainThread {
                            for (tempInfo in list) {
                                for (i in 0 until downloadInfoBundle.list.size) {
                                    val info = downloadInfoBundle.list.get(i)
                                    if (info.downloadId == tempInfo.downloadId) {
                                        info.sizeTotal = tempInfo.sizeTotal
                                        info.sizeSoFar = tempInfo.sizeSoFar

                                        downloadInfoBundle.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_CHANGED
                                        downloadInfoBundle.index = i.toLong()
                                        value = downloadInfoBundle
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        // no running items, remove update
                        cleanUp()
                    }
                }
            } catch (e: Exception) {
            } finally {
                CursorUtils.closeCursorSafely(cursor)
            }
        }
        return true
    }

    private fun cleanUp() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    companion object {
        private val QUERY_PROGRESS_DELAY: Long = 500
        private val PAGE_SIZE = 20
    }
}
