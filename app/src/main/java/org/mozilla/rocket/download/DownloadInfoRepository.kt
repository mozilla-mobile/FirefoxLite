package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.LiveData
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager

class DownloadInfoRepository {

    var downloadIndicator: DownloadIndicatorLiveData? = null
    var downloadInfoBundle: DownloadInfoLiveData? = null

    fun getDownloadIndicator(): LiveData<Int>? {
        return downloadIndicator
    }

    fun updateIndicator() {
        downloadIndicator?.forceQuery()
    }

    fun getDownloadInfoBundle(): LiveData<DownloadInfoBundle>? {
        return downloadInfoBundle
    }

    fun cancelDownload(id: Long, listener: DownloadInfoViewModel.OnOperationComplete) {
        downloadInfoBundle?.cancel(id, listener)
    }

    fun deleteDownload(id: Long, listener: DownloadInfoViewModel.OnOperationComplete) {
        downloadInfoBundle?.delete(id, listener)
    }

    fun removeDownload(id: Long) {
        downloadInfoBundle?.removeItem(id)
    }

    fun hideDownload(id: Long) {
        downloadInfoBundle?.hideItem(id)
    }

    fun addDownload(downloadInfo: DownloadInfo) {
        downloadInfoBundle?.addItem(downloadInfo)
    }

    fun loadMore(init: Boolean) {
        downloadInfoBundle?.loadMore(init)
    }

    fun isOpening(): Boolean? {
        return downloadInfoBundle?.isOpening
    }

    fun setOpening(value: Boolean) {
        downloadInfoBundle?.isOpening = value
    }

    fun markAllItemsAreRead() {
        DownloadInfoManager.getInstance().markAllItemsAreRead(null)
    }

    companion object {

        @Volatile private var INSTANCE: DownloadInfoRepository? = null

        @JvmStatic
        fun getInstance(application: Application): DownloadInfoRepository? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: DownloadInfoRepository().also {
                        INSTANCE = it
                        INSTANCE?.downloadIndicator = DownloadIndicatorLiveData(application.applicationContext)
                        INSTANCE?.downloadInfoBundle = DownloadInfoLiveData(application.applicationContext)
                    }
                }
    }
}
