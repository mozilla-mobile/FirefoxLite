package org.mozilla.rocket.download

import android.app.DownloadManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.rocket.download.data.DownloadInfo
import org.mozilla.rocket.download.data.DownloadInfoRepository
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

    private val runningDownloadIds: LongArray
        get() {
            val ids = downloadInfoPack.list
                    .filter { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }
                    .mapNotNull { it.downloadId }
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

        viewModelScope.launch {
            val list = repository.loadData(itemCount, PAGE_SIZE)
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
    }

    fun cancel(rowId: Long) = viewModelScope.launch {
        repository.queryByRowId(rowId)?.let { download ->
            if (download.existInDownloadManager()) {
                val downloadId = download.downloadId
                if (rowId == download.rowId && DownloadManager.STATUS_SUCCESSFUL != download.status && downloadId != null) {
                    toastMessageObservable.value = R.string.download_cancel
                    repository.trackDownloadCancel(downloadId)
                    repository.deleteFromDownloadManager(downloadId)
                    remove(rowId)
                }
            }
        }
    }

    fun remove(rowId: Long) = viewModelScope.launch {
        repository.remove(rowId)
        hide(rowId)
    }

    fun delete(rowId: Long) = viewModelScope.launch {
        repository.queryByRowId(rowId)?.let { download ->
            withContext(Dispatchers.IO) {
                val file =
                    try {
                        File(URI(download.fileUri).path)
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                        null
                    }
                if (file?.exists() == true) {
                    deleteSnackbarObservable.postValue(download)
                } else {
                    toastMessageObservable.postValue(R.string.cannot_find_the_file)
                }
            }
        }
    }

    fun confirmDelete(download: DownloadInfo) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val deleteFile = File(URI(download.fileUri).path)
            if (deleteFile.delete()) {
                download.downloadId?.let { repository.deleteFromDownloadManager(it) }
                download.rowId?.let { repository.remove(it) }
            } else {
                toastMessageObservable.postValue(R.string.cannot_delete_the_file)
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "" + e.message)
            toastMessageObservable.postValue(R.string.cannot_delete_the_file)
        }
    }

    fun add(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoPack.list.size) {
            if ((downloadInfoPack.list[i].rowId ?: -1) < (downloadInfo.rowId ?: -1)) {
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

    private fun updateRunningItems() = viewModelScope.launch {
        if (runningDownloadIds.isNotEmpty()) {
            for (i in runningDownloadIds.indices) {
                repository.queryByDownloadId(runningDownloadIds[i])?.let { download ->
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
            }
        }
    }

    fun notifyDownloadComplete(downloadId: Long) = viewModelScope.launch {
        repository.queryByDownloadId(downloadId)?.let { download ->
            if (download.existInDownloadManager()) {
                updateItem(download)
            }
        }
    }

    fun notifyRowUpdate(rowId: Long) = viewModelScope.launch {
        repository.queryByRowId(rowId)?.let {
            updateItem(it)
        }
    }

    fun queryDownloadProgress() = viewModelScope.launch {
        val list = repository.queryDownloadingItems(runningDownloadIds)
        if (list.isNotEmpty()) {
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
            progressUpdateListener?.onCompleteUpdate()
        } else {
            // no running items, remove update
            progressUpdateListener?.onStopUpdate()
        }
    }

    fun markAllItemsAreRead() = viewModelScope.launch {
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
