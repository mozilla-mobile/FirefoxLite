package org.mozilla.rocket.download

import android.app.DownloadManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import org.mozilla.focus.R
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.threadutils.ThreadUtils
import java.io.File
import java.net.URI
import java.net.URISyntaxException

class DownloadInfoViewModel(private val repository: DownloadInfoRepository) : ViewModel() {

    interface OnProgressUpdateListener {
        fun onStartUpdate()
        fun onCompleteUpdate()
        fun onStopUpdate()
    }

    val downloadInfoObservable = MutableLiveData<DownloadInfoPack>()
    val toastMessageObservable = SingleLiveEvent<Int>()
    val deleteSnackbarObservable = SingleLiveEvent<DownloadInfo>()
    var isOpening = false

    private val downloadInfoPack: DownloadInfoPack = DownloadInfoPack(ArrayList(), -1, -1)

    private var itemCount = 0
    private var isLoading = false
    private var isLastPage = false
    private var progressUpdateListener: OnProgressUpdateListener? = null

    private var updateListener: DownloadInfoRepository.OnQueryItemCompleteListener = object : DownloadInfoRepository.OnQueryItemCompleteListener {
        override fun onComplete(download: DownloadInfo) {
            updateItem(download)
        }
    }

    private val runningDownloadIds: LongArray
        get() {
            val ids = downloadInfoPack.list
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
            return downloadInfoPack.list.any { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }
        }

    private fun updateItem(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoPack.list.size) {
            if (downloadInfoPack.list[i].rowId == downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            downloadInfoPack.list.add(0, downloadInfo)
        } else {
            downloadInfoPack.list.removeAt(index)
            downloadInfoPack.list.add(index, downloadInfo)
        }
        downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED
        downloadInfoObservable.value = downloadInfoPack
    }

    fun loadMore(init: Boolean) {
        // Once the "Don't keep activity" in developer settings is enabled, loadMore is called twice continuously
        // due to DownloadFragment is created twice. So we set isLoading immediately to prevent duplicate calls here.
        if (isLoading) {
            return
        }
        isLoading = true
        if (init) {
            isLastPage = false
            isOpening = false
            itemCount = 0
            downloadInfoPack.list.clear()
        }
        if (isLastPage) {
            isLoading = false
            return
        }
        repository.loadData(itemCount, PAGE_SIZE, object : DownloadInfoRepository.OnQueryListCompleteListener {
            override fun onComplete(list: List<DownloadInfo>) {
                downloadInfoPack.list.addAll(list)
                downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED
                itemCount = downloadInfoPack.list.size
                downloadInfoObservable.value = downloadInfoPack
                isOpening = false
                isLoading = false
                isLastPage = list.isEmpty()
                if (isDownloading) {
                    progressUpdateListener?.onStartUpdate()
                }
            }
        })
    }

    fun cancel(rowId: Long) {
        repository.queryByRowId(rowId, object : DownloadInfoRepository.OnQueryItemCompleteListener {
            override fun onComplete(download: DownloadInfo) {
                if (download.existInDownloadManager()) {
                    if (rowId == download.rowId && DownloadManager.STATUS_SUCCESSFUL != download.status) {
                        toastMessageObservable.value = R.string.download_cancel
                        repository.deleteFromDownloadManager(download.downloadId)
                        remove(rowId)
                    }
                }
            }
        })
    }

    fun remove(rowId: Long) {
        repository.remove(rowId)
        hide(rowId)
    }

    fun delete(rowId: Long) {
        repository.queryByRowId(rowId, object : DownloadInfoRepository.OnQueryItemCompleteListener {
            override fun onComplete(download: DownloadInfo) {
                val file = try {
                    File(URI(download.fileUri).path)
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                    null
                }
                if (file?.exists() == true) {
                    deleteSnackbarObservable.value = download
                } else {
                    toastMessageObservable.value = R.string.cannot_find_the_file
                }
            }
        })
    }

    fun confirmDelete(download: DownloadInfo) {
        try {
            val deleteFile = File(URI(download.fileUri).path)
            if (deleteFile.delete()) {
                repository.deleteFromDownloadManager(download.downloadId)
                repository.remove(download.rowId)
            } else {
                toastMessageObservable.value = R.string.cannot_delete_the_file
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "" + e.message)
            toastMessageObservable.value = R.string.cannot_delete_the_file
        }
    }

    fun add(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoPack.list.size) {
            if (downloadInfoPack.list[i].rowId < downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            downloadInfoPack.list.add(downloadInfo)
            // The crash will happen when data set size is 1 after add item.
            // Because we define item count is 1 and mDownloadInfo is empty that means nothing and show empty view.
            // So use notifyDataSetChanged() instead of notifyItemInserted when data size is 1 after add item.
            if (downloadInfoPack.list.size > 1) {
                downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_ITEM_INSERTED
                downloadInfoPack.index = (downloadInfoPack.list.size - 1).toLong()
            } else {
                downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED
            }
        } else {
            downloadInfoPack.list.add(index, downloadInfo)
            downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_ITEM_INSERTED
            downloadInfoPack.index = index.toLong()
        }
        downloadInfoObservable.value = downloadInfoPack
    }

    fun hide(rowId: Long) {
        for (i in 0 until downloadInfoPack.list.size) {
            val downloadInfo = downloadInfoPack.list[i]
            if (rowId == downloadInfo.rowId) {
                downloadInfoPack.list.remove(downloadInfo)
                downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_ITEM_REMOVED
                downloadInfoPack.index = i.toLong()
                downloadInfoObservable.value = downloadInfoPack
                break
            }
        }
    }

    private fun updateRunningItems() {
        if (!runningDownloadIds.isEmpty()) {
            for (i in runningDownloadIds.indices) {
                repository.queryByDownloadId(runningDownloadIds[i], object : DownloadInfoRepository.OnQueryItemCompleteListener {
                    override fun onComplete(download: DownloadInfo) {
                        for (j in 0 until downloadInfoPack.list.size) {
                            val downloadInfo = downloadInfoPack.list[j]
                            if (download.downloadId == downloadInfo.downloadId) {
                                downloadInfo.setStatusInt(download.status)
                                downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_ITEM_CHANGED
                                downloadInfoPack.index = j.toLong()
                                downloadInfoObservable.value = downloadInfoPack
                            }
                        }
                    }
                })
            }
        }
    }

    fun notifyDownloadComplete(downloadId: Long) {
        repository.queryByDownloadId(downloadId, updateListener)
    }

    fun notifyRowUpdate(rowId: Long) {
        repository.queryByRowId(rowId, updateListener)
    }

    fun queryDownloadProgress() {
        repository.queryDownloadingItems(runningDownloadIds, object : DownloadInfoRepository.OnQueryListCompleteListener {
            override fun onComplete(list: List<DownloadInfo>) {
                if (list.isNotEmpty()) {
                    ThreadUtils.postToMainThread {
                        for (tempInfo in list) {
                            for (i in 0 until downloadInfoPack.list.size) {
                                val info = downloadInfoPack.list.get(i)
                                if (info.downloadId == tempInfo.downloadId) {
                                    info.sizeTotal = tempInfo.sizeTotal
                                    info.sizeSoFar = tempInfo.sizeSoFar

                                    downloadInfoPack.notifyType = DownloadInfoPack.Constants.NOTIFY_ITEM_CHANGED
                                    downloadInfoPack.index = i.toLong()
                                    downloadInfoObservable.value = downloadInfoPack
                                    break
                                }
                            }
                        }
                    }
                    progressUpdateListener?.onCompleteUpdate()
                } else {
                    // no running items, remove update
                    progressUpdateListener?.onStopUpdate()
                }
            }
        })
    }

    fun markAllItemsAreRead() {
        repository.markAllItemsAreRead()
    }

    fun registerForProgressUpdate(listener: OnProgressUpdateListener) {
        progressUpdateListener = listener
        // When somebody is listening progress update, we need to notify they to start to update
        if (isDownloading) {
            progressUpdateListener?.onStartUpdate()
        }
        updateRunningItems()
    }

    fun unregisterForProgressUpdate() {
        progressUpdateListener?.onStopUpdate()
        progressUpdateListener = null
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}