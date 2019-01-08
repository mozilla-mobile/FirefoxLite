package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import org.mozilla.focus.download.DownloadInfo

class DownloadInfoViewModel(application: Application, private val repository: DownloadInfoRepository) : AndroidViewModel(application) {

    // TODO refactor with coroutine is better
    interface OnOperationComplete {
        fun onComplete(downloadInfo: DownloadInfo)
    }

    val downloadInfoBundle: LiveData<DownloadInfoBundle>?
        get() = repository.getDownloadInfoBundle()

    fun cancelDownload(id: Long, listener: OnOperationComplete) {
        repository.cancelDownload(id, listener)
    }

    fun delete(id: Long, listener: OnOperationComplete) {
        repository.deleteDownload(id, listener)
    }

    fun removeDownload(id: Long) {
        repository.removeDownload(id)
    }

    fun hideDownload(id: Long) {
        repository.hideDownload(id)
    }

    fun addDownload(downloadInfo: DownloadInfo) {
        repository.addDownload(downloadInfo)
    }

    fun loadMore(init: Boolean) {
        repository.loadMore(init)
    }

    fun isOpening(): Boolean? {
        return repository.isOpening()
    }

    fun setOpening(value: Boolean) {
        repository.setOpening(value)
    }

    fun markAllItemsAreRead() {
        repository.markAllItemsAreRead()
    }
}