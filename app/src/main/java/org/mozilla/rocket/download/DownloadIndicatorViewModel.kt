package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.download.DownloadInfo

class DownloadIndicatorViewModel(private val repository: DownloadInfoRepository) : ViewModel() {

    enum class Status {
        DEFAULT, DOWNLOADING, UNREAD
    }

    val downloadIndicatorObservable = MutableLiveData<Status>()

    fun updateIndicator() {
        repository.queryIndicatorStatus(object : DownloadInfoRepository.OnQueryListCompleteListener {
            override fun onComplete(list: List<DownloadInfo>) {
                var hasDownloading = false
                var hasUnread = false
                for (item in list) {
                    if (!hasDownloading && (item.status == DownloadManager.STATUS_RUNNING ||
                                item.status == DownloadManager.STATUS_PENDING ||
                                item.status == DownloadManager.STATUS_PAUSED)) {
                        hasDownloading = true
                    }
                    if (!hasUnread && item.status == DownloadManager.STATUS_SUCCESSFUL && !item.isRead) {
                        hasUnread = true
                    }
                }
                downloadIndicatorObservable.value = when {
                    hasDownloading -> Status.DOWNLOADING
                    hasUnread -> Status.UNREAD
                    else -> Status.DEFAULT
                }
            }
        })
    }
}
