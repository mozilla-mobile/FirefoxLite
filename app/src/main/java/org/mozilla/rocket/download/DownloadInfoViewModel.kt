package org.mozilla.rocket.download

import android.app.Application
import android.app.DownloadManager
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.os.Handler
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager

class DownloadInfoViewModel(application: Application, private val repository: DownloadInfoRepository) :
    AndroidViewModel(application) {

    var downloadInfoBundle = MediatorLiveData<DownloadInfoBundle>()

    var isOpening = false

    private val isDownloading: Boolean
        get() {
            return downloadInfoBundle.value?.list!!.any { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }
        }

    private var handler: Handler? = null
    private var itemCount = 0
    private var isLoading = false
    private var isLastPage = false

    // TODO refactor with coroutine is better
    interface OnOperationComplete {
        fun onComplete(downloadInfo: DownloadInfo)
    }

    fun cancelDownload(id: Long, listener: OnOperationComplete) {
        repository.cancel(id, listener)
        hideDownload(id)
    }

    fun delete(id: Long, listener: OnOperationComplete) {
        repository.delete(id, listener)
        hideDownload(id)
    }

    fun removeDownload(id: Long) {
        repository.removeItem(id)
        hideDownload(id)
    }

    fun hideDownload(rowId: Long) {
        for (i in 0 until downloadInfoBundle.value!!.list.size) {
            val downloadInfo = downloadInfoBundle.value!!.list[i]
            if (rowId == downloadInfo.rowId) {
                downloadInfoBundle.value!!.list.remove(downloadInfo)
                downloadInfoBundle.value!!.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_REMOVED
                downloadInfoBundle.value!!.index = i.toLong()
                uglyNotifyLiveDataChange()
                break
            }
        }
    }

    fun addDownload(downloadInfo: DownloadInfo) {
        var index = -1
        for (i in 0 until downloadInfoBundle.value!!.list.size) {
            if (downloadInfoBundle.value!!.list[i].rowId < downloadInfo.rowId) {
                index = i
                break
            }
        }

        if (index == -1) {
            downloadInfoBundle.value!!.list.add(downloadInfo)
            //The crash will happen when data set size is 1 after add item.
            //Because we define item count is 1 and mDownloadInfo is empty that means nothing and show empty view.
            //So use notifyDataSetChanged() instead of notifyItemInserted when data size is 1 after add item.
            if (downloadInfoBundle.value!!.list.size > 1) {
                downloadInfoBundle.value!!.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED
                downloadInfoBundle.value!!.index = (downloadInfoBundle.value!!.list.size - 1).toLong()
            } else {
                downloadInfoBundle.value!!.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
            }
        } else {
            downloadInfoBundle.value!!.list.add(index, downloadInfo)
            downloadInfoBundle.value!!.notifyType = DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED
            downloadInfoBundle.value!!.index = index.toLong()
        }
        uglyNotifyLiveDataChange()    }

    fun loadMore(init: Boolean) {
        if (init) {
            isLoading = false
            isLastPage = false
            isOpening = false
            itemCount = 0
            downloadInfoBundle.value?.list?.clear()
        }
        if (isLastPage || isLoading) {
            return
        }
        repository.loadMore(itemCount, DownloadInfoManager.AsyncQueryListener { downloadInfoList ->
            downloadInfoBundle.value?.list?.addAll(downloadInfoList)
            downloadInfoBundle.value?.notifyType = DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED
            itemCount = downloadInfoBundle.value?.list?.size ?: 0
            uglyNotifyLiveDataChange()
            isOpening = false
            isLoading = false
            isLastPage = downloadInfoList.size == 0
            if (isDownloading && handler == null) {

                val runningIds = runningDownloadIds
                if (runningIds.isEmpty()) {
                    cleanUp()
                    // nothing is running, don't need to update progress
                    return@AsyncQueryListener
                }

                // FIXME: use Coroutine
                handler = Handler()

                handler?.post(object : Runnable {
                    override fun run() {

                        if (repository.queryDownloadProgress(runningDownloadIds, updateProgress)) {
                            handler?.postDelayed(this, DownloadInfoRepository.QUERY_PROGRESS_DELAY)
                        } else {
                            cleanUp()
                        }
                    }
                })
            }
        })
        isLoading = true
    }

    fun markAllItemsAreRead() {
        repository.markAllItemsAreRead()
    }

    private val updateProgress: (ArrayList<DownloadInfo>) -> Unit = {
        for (tempInfo in it) {
            for (i in 0 until downloadInfoBundle.value?.list!!.size) {
                val info = downloadInfoBundle.value!!.list.get(i)
                if (info.downloadId == tempInfo.downloadId) {
                    info.sizeTotal = tempInfo.sizeTotal
                    info.sizeSoFar = tempInfo.sizeSoFar

                    downloadInfoBundle.value?.notifyType =
                            DownloadInfoBundle.Constants.NOTIFY_ITEM_CHANGED
                    downloadInfoBundle.value?.index = i.toLong()
                    uglyNotifyLiveDataChange()
                    break
                }
            }
        }
    }

    private val runningDownloadIds: LongArray
        get() {
            val ids =
                downloadInfoBundle.value?.list?.filter { it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING }!!.map { it.downloadId }
            val array = LongArray(ids.size)
            for (i in array.indices) {
                array[i] = ids[i]
            }

            return array
        }

    private fun cleanUp() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    // this is stupid. fix it
    private fun uglyNotifyLiveDataChange() {
        downloadInfoBundle.value = downloadInfoBundle.value
    }
}