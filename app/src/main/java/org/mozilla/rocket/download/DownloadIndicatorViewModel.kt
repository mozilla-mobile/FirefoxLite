package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.download.DownloadInfo

class DownloadIndicatorViewModel(private val repository: DownloadInfoRepository, val downloadIndicatorObservable: MutableLiveData<Status>) : ViewModel() {

    enum class Status {
        DEFAULT, DOWNLOADING, UNREAD, WARNING
    }

    fun updateIndicator() {
        repository.queryIndicatorStatus(object : DownloadInfoRepository.OnQueryListCompleteListener {
            override fun onComplete(list: List<DownloadInfo>) {
                var hasDownloading = false
                var hasUnread = false
                var hasWarning = false
                for (item in list) {
                    if (!hasDownloading && (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING)) {
                        hasDownloading = true
                    }
                    if (!hasUnread && item.status == DownloadManager.STATUS_SUCCESSFUL && !item.isRead) {
                        hasUnread = true
                    }
                    if (!hasWarning && (item.status == DownloadManager.STATUS_PAUSED || item.status == DownloadManager.STATUS_FAILED)) {
                        hasWarning = true
                    }
                }
                downloadIndicatorObservable.value = when {
                    hasDownloading -> Status.DOWNLOADING
                    hasUnread -> Status.UNREAD
                    hasWarning -> Status.WARNING
                    else -> Status.DEFAULT
                }
            }
        })
    }
}
