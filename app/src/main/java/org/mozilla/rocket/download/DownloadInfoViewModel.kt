package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import org.mozilla.focus.download.DownloadInfo

class DownloadInfoViewModel(application: Application, private val repository: DownloadInfoRepository) :
    AndroidViewModel(application) {

    // TODO refactor with coroutine is better
    interface OnOperationComplete {
        fun onComplete(downloadInfo: DownloadInfo)
    }

    val downloadInfoBundle = repository.downloadInfoBundle

    fun cancelDownload(id: Long, listener: OnOperationComplete) {
        repository.cancel(id, listener)
    }

    fun delete(id: Long, listener: OnOperationComplete) {
        repository.delete(id, listener)
    }

    fun removeDownload(id: Long) {
        repository.removeItem(id)
    }

    fun hideDownload(id: Long) {
        repository.hideItem(id)
    }

    fun addDownload(downloadInfo: DownloadInfo) {
        repository.addItem(downloadInfo)
    }

    fun loadMore(init: Boolean) {
        repository.loadMore(init)
    }

    fun isOpening(): Boolean? {
        return repository.isOpening
    }

    fun setOpening(value: Boolean) {
        repository.isOpening = value
    }

    fun markAllItemsAreRead() {
        repository.markAllItemsAreRead()
    }
}